package me.paulbaur.ict.target.service;

import me.paulbaur.ict.target.domain.Target;

import java.util.List;
import java.util.UUID;

public interface TargetService {

    List<Target> findAll();

    Target create(Target target);

    void delete(UUID id);
}
