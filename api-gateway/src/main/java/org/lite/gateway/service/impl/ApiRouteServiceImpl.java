package org.lite.gateway.service.impl;

import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.repository.ApiRouteRepository;
import org.lite.gateway.service.RouteService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ApiRouteServiceImpl implements RouteService {
    private final ApiRouteRepository apiRouteRepository;

    public ApiRouteServiceImpl(ApiRouteRepository apiRouteRepository) {
        this.apiRouteRepository = apiRouteRepository;
    }

    @Override
    public Flux<ApiRoute> getAll() {
        return this.apiRouteRepository.findAll();
    }

    @Override
    public Mono<ApiRoute> create(ApiRoute apiRoute) {
        return this.apiRouteRepository.save(apiRoute);
    }

    @Override
    public Mono<ApiRoute> getById(String id) {
        return this.apiRouteRepository.findById(id);
    }
}
