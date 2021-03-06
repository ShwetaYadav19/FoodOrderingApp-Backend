package com.upgrad.FoodOrderingApp.service.businness;

import java.time.ZonedDateTime;
import com.upgrad.FoodOrderingApp.service.dao.CustomerAuthDao;
import com.upgrad.FoodOrderingApp.service.dao.CustomerDao;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.UpdateCustomerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonCustomerService {

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private CustomerAuthDao customerAuthDao;

    @Autowired
    private CustomerAuthenticationService customerAuthenticationService;

    @Autowired
    private PasswordCryptographyProvider passwordCryptographyProvider;

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity getCustomerByContactNumber(final String contactNumber) {
        CustomerEntity customerEntity = customerDao.getCustomerByContactNumber(contactNumber);
        return customerEntity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity getCustomerByUuid(final String uuid) {
        CustomerEntity customerEntity = customerDao.getCustomerByUuid(uuid);
        return customerEntity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerAuthEntity getCustomerAuthByAccessToken(final String accessToken) {
        CustomerAuthEntity customerAuthEntity = customerAuthDao.getCustomerAuthByToken(accessToken);
        return customerAuthEntity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity updateCustomer(final String accessToken, final String updatedFirstName,
                               final String updatedLastName) throws AuthorizationFailedException {
        CustomerAuthEntity customerAuthEntity = customerAuthDao.getCustomerAuthByToken(accessToken);
        if(customerAuthEntity==null){
            throw new AuthorizationFailedException("ATHR-001","Customer is not Logged in");
        }

        if(customerAuthEntity.getLogoutAt()!=null){
            throw new AuthorizationFailedException("ATHR-002","Customer is logged out. Log in again to access this endpoint.");
        }

        if(customerAuthEntity.getExpiresAt().isBefore(ZonedDateTime.now())){
            throw new AuthorizationFailedException("ATHR-003","Your session is expired. Log in again to access this endpoint.");
        }

        CustomerEntity customerEntity = customerAuthEntity.getCustomerEntity();
        customerEntity.setFirstName(updatedFirstName);
        customerEntity.setLastName(updatedLastName);
        customerDao.updateCustomerEntity(customerEntity);
        return customerDao.getCustomerByUuid(customerEntity.getUuid());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity updatePassword(String accessToken, String oldPassword, String newPassword) throws AuthorizationFailedException, UpdateCustomerException {
        CustomerAuthEntity customerAuthEntity = customerAuthDao.getCustomerAuthByToken(accessToken);
        if(customerAuthEntity==null){
            throw new AuthorizationFailedException("ATHR-001","Customer is not Logged in");
        }

        if(customerAuthEntity.getLogoutAt()!=null){
            throw new AuthorizationFailedException("ATHR-002","Customer is logged out. Log in again to access this endpoint.");
        }

        if(customerAuthEntity.getExpiresAt().isBefore(ZonedDateTime.now())){
            throw new AuthorizationFailedException("ATHR-003","Your session is expired. Log in again to access this endpoint.");
        }


        if(customerAuthenticationService.isPasswordWeak(newPassword)){
            throw new UpdateCustomerException("UCR-001","Weak password!");
        }
        final String encryptedOldPassword =
                passwordCryptographyProvider.encrypt(oldPassword, customerAuthEntity.getCustomerEntity().getSalt());

        if(!customerAuthEntity.getCustomerEntity().getPassword().equals(encryptedOldPassword)){
            throw new UpdateCustomerException("UCR-004","Incorrect old password!");
        }
        CustomerEntity customerEntity = customerAuthEntity.getCustomerEntity();
        String[] encryptedNewPassword = passwordCryptographyProvider.encrypt(newPassword);

        customerEntity.setSalt(encryptedNewPassword[0]);
        customerEntity.setPassword(encryptedNewPassword[1]);
        customerDao.updateCustomerEntity(customerEntity);

        customerAuthEntity.setLogoutAt(ZonedDateTime.now());
        customerAuthDao.updateCustomerAuth(customerAuthEntity);
        return customerDao.getCustomerByUuid(customerEntity.getUuid());

    }
}
