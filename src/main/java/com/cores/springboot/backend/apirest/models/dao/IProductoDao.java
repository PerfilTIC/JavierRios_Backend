package com.cores.springboot.backend.apirest.models.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cores.springboot.backend.apirest.models.entity.Producto;

public interface IProductoDao extends JpaRepository<Producto, Long> {

}
