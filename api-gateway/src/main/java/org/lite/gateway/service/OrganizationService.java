package org.lite.gateway.service;

import org.lite.gateway.dto.OrganizationDTO;
import org.lite.gateway.entity.Organization;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrganizationService {
    Flux<OrganizationDTO> getAllOrganizations();
    Mono<OrganizationDTO> getOrganizationById(String id);
    Mono<OrganizationDTO> createOrganization(Organization organization);
    Mono<OrganizationDTO> updateOrganization(String id, Organization organization);
    Mono<Void> deleteOrganization(String id);
    Flux<OrganizationDTO> searchOrganizations(String query);
    Mono<OrganizationDTO> convertToDTO(Organization organization);
} 