package com.upgrad.FoodOrderingApp.service.dao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import org.springframework.stereotype.Repository;


@Repository
public class CustomerDao {

    @PersistenceContext
    private EntityManager entityManager;


    /**
     * This methods stores the Customer details in the DB. This method receives the object of Customer
     * type with its attributes being set.
     */
    public CustomerEntity createCustomer(CustomerEntity customerEntity) {
        entityManager.persist(customerEntity);
        return customerEntity;
    }


    public CustomerEntity getCustomerByContactNumber(final String contactNumber) {
        try {
            return entityManager
                    .createNamedQuery("customerByContactNumber", CustomerEntity.class)
                    .setParameter("contactNumber", contactNumber)
                    .getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }

    }
}
