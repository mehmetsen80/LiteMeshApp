package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.dto.OrganizationDTO;
import org.lite.gateway.entity.Organization;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.lite.gateway.exception.OrganizationOperationException;
import org.lite.gateway.repository.OrganizationRepository;
import org.lite.gateway.repository.TeamRepository;
import org.lite.gateway.service.OrganizationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Flux<OrganizationDTO> getAllOrganizations() {
        return organizationRepository.findAll()
            .flatMap(this::convertToDTO);
    }

    @Override
    public Mono<OrganizationDTO> getOrganizationById(String id) {
        return organizationRepository.findById(id)
            .flatMap(this::convertToDTO)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                String.format("Organization with id %s not found", id), ErrorCode.ORGANIZATION_NOT_FOUND
            )));
    }

    @Override
    public Mono<OrganizationDTO> createOrganization(Organization organization) {
        organization.setName(organization.getName().trim());
        
        Mono<OrganizationDTO> createOrgMono = organizationRepository.save(organization)
            .flatMap(this::convertToDTO)
            .doOnSuccess(org -> log.info("Organization created successfully: {}", org.getName()))
            .onErrorMap(DuplicateKeyException.class, ex -> {
                log.error("Organization creation failed - duplicate name: {}", organization.getName());
                return new IllegalArgumentException("An organization with this name already exists");
            });

        return transactionalOperator.execute(status -> createOrgMono)
            .single()
            .doOnSuccess(response -> log.info("Transaction completed successfully for organization: {}", response.getName()))
            .doOnError(e -> log.error("Transaction failed while creating organization: {}", e.getMessage()));
    }

    @Override
    public Mono<OrganizationDTO> updateOrganization(String id, Organization organization) {
        organization.setName(organization.getName().trim());
        
        Mono<OrganizationDTO> updateOrgMono = organizationRepository.findById(id)
            .flatMap(existingOrg -> {
                existingOrg.setName(organization.getName());
                existingOrg.setDescription(organization.getDescription());
                return organizationRepository.save(existingOrg);
            })
            .flatMap(this::convertToDTO)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                String.format("Organization with id %s not found", id), 
                ErrorCode.ORGANIZATION_NOT_FOUND
            )))
            .doOnSuccess(org -> log.info("Organization updated successfully: {}", org.getName()))
            .onErrorMap(DuplicateKeyException.class, ex -> {
                log.error("Organization update failed - duplicate name: {}", organization.getName());
                return new IllegalArgumentException("An organization with this name already exists");
            });

        return transactionalOperator.execute(status -> updateOrgMono)
            .single()
            .doOnSuccess(response -> log.info("Transaction completed successfully for organization update: {}", response.getName()))
            .doOnError(e -> log.error("Transaction failed while updating organization: {}", e.getMessage()));
    }

    @Override
    public Mono<Void> deleteOrganization(String id) {
        Mono<Void> deleteOrgMono = organizationRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                String.format("Organization with id %s not found", id), 
                ErrorCode.ORGANIZATION_NOT_FOUND
            )))
            .flatMap(organization -> 
                teamRepository.findByOrganizationId(id)
                    .collectList()
                    .flatMap(teams -> {
                        if (!teams.isEmpty()) {
                            log.error("Cannot delete organization {} - has {} assigned teams", id, teams.size());
                            return Mono.error(new OrganizationOperationException(
                                "Cannot delete organization with assigned teams"
                            ));
                        }
                        log.info("Deleting organization: {}", organization.getName());
                        return organizationRepository.deleteById(id);
                    })
            )
            .doOnSuccess(v -> log.info("Organization {} deleted successfully", id));

        return transactionalOperator.execute(status -> deleteOrgMono)
            .then()
            .doOnSuccess(v -> log.info("Transaction completed successfully for organization deletion"))
            .doOnError(e -> log.error("Transaction failed while deleting organization: {}", e.getMessage()));
    }

    @Override
    public Flux<OrganizationDTO> searchOrganizations(String query) {
        return organizationRepository.findByNameContainingIgnoreCase(query)
            .flatMap(this::convertToDTO);
    }

    @Override
    public Mono<OrganizationDTO> convertToDTO(Organization organization) {
        return teamRepository.findByOrganizationId(organization.getId())
            .collectList()
            .map(teams -> OrganizationDTO.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .createdBy(organization.getCreatedBy())
                .updatedBy(organization.getUpdatedBy())
                .teamCount(teams.size())
                .build());
    }
} 