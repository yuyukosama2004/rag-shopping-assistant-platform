package com.biyesheji.user.service;

import com.biyesheji.entity.Address;

import java.util.List;

public interface AddressService {
    Address add(Long userId, Address address);
    Address update(Long userId, Address address);
    void delete(Long userId, Long addressId);
    Address getById(Long id);
    List<Address> listByUserId(Long userId);
    void setDefault(Long userId, Long addressId);
}
