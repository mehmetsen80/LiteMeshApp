package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.dto.OrganizationDTO;
import org.lite.gateway.entity.Organization;
import org.lite.gateway.exception.ResourceNotFoundException;
import org.lite.gateway.exception.OrganizationOperationException;
import org.lite.gateway.repository.OrganizationRepository;
import org.lite.gateway.repository.TeamRepository;
import org.lite.gateway.service.OrganizationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;

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
                String.format("Organization with id %s not found", id)
            )));
    }

    @Override
    public Mono<OrganizationDTO> createOrganization(Organization organization) {
        organization.setName(organization.getName().trim());
        return organizationRepository.save(organization)
            .flatMap(this::convertToDTO)
            .onErrorMap(DuplicateKeyException.class, ex ->
                new IllegalArgumentException("An organization with this name already exists")
            );
    }

    @Override
    public Mono<OrganizationDTO> updateOrganization(String id, Organization organization) {
        organization.setName(organization.getName().trim());
        return organizationRepository.findById(id)
            .flatMap(existingOrg -> {
                existingOrg.setName(organization.getName());
                existingOrg.setDescription(organization.getDescription());
                return organizationRepository.save(existingOrg);
            })
            .flatMap(this::convertToDTO)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                String.format("Organization with id %s not found", id)
            )))
            .onErrorMap(DuplicateKeyException.class, ex ->
                new IllegalArgumentException("An organization with this name already exists")
            );
    }

    @Override
    public Mono<Void> deleteOrganization(String id) {
        return organizationRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                String.format("Organization with id %s not found", id)
            )))
            .flatMap(organization -> 
                teamRepository.findByOrganizationId(id)
                    .collectList()
                    .flatMap(teams -> {
                        if (!teams.isEmpty()) {
                            return Mono.error(new OrganizationOperationException(
                                "Cannot delete organization with assigned teams"
                            ));
                        }
                        return organizationRepository.deleteById(id);
                    })
            );
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