package com.upgrad.FoodOrderingApp.service.businness;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.upgrad.FoodOrderingApp.service.dao.CustomerAuthDao;
import com.upgrad.FoodOrderingApp.service.dao.CustomerDao;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAuthenticationService {

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private CustomerAuthDao customerAuthDao;

    @Autowired
    private PasswordCryptographyProvider passwordCryptographyProvider;


    /**
     * This method checks if the contact number exist in the DB,if any field other than last name is empty or not,
     * if email  and contact number are valid
     *  and password is not weak
     *
     *  assign uuid to the user. Assign encrypted password and salt to the user.
     *
     *
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity signup(CustomerEntity customerEntity) throws SignUpRestrictedException {
        if (isContactNumberAlreadyExists(customerEntity.getContactNumber())) {
            throw new SignUpRestrictedException(
                    "SGR-001", "This contact number is already registered! Try other contact number.");
        }

        if (isFieldOtherThanLastnameEmpty(customerEntity)) {
            throw new SignUpRestrictedException(
                    "SGR-005", "Except last name all fields should be filled");
        }

        if (isEmailNotInCorrectFormat(customerEntity.getEmail())) {
            throw new SignUpRestrictedException(
                    "SGR-002", "Invalid email-id format!");
        }

        if (isContactNumberNotInCorrectFormat(customerEntity.getContactNumber())) {
            throw new SignUpRestrictedException(
                    "SGR-003", "Invalid contact number!");
        }

        if (isPasswordWeak(customerEntity.getPassword())) {
            throw new SignUpRestrictedException(
                    "SGR-004", "Weak password!");
        }

        // Assign a UUID to the customer that is being created.
        customerEntity.setUuid(UUID.randomUUID().toString());
        // Assign encrypted password and salt to the customer that is being created.
        String[] encryptedText = passwordCryptographyProvider.encrypt(customerEntity.getPassword());
        customerEntity.setSalt(encryptedText[0]);
        customerEntity.setPassword(encryptedText[1]);
        return customerDao.createCustomer(customerEntity);

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerAuthEntity login(final String contactNumber, final String password) throws AuthenticationFailedException {
        CustomerEntity customerEntity = customerDao.getCustomerByContactNumber(contactNumber);

        if(contactNumber.isEmpty() || password.isEmpty()){
            throw new AuthenticationFailedException("ATH-003","Incorrect format of decoded customer name and password");
        }

        if (customerEntity == null) {
            throw new AuthenticationFailedException("ATH-001", "This contact number has not been registered!");
        }
        final String encryptedPassword =
                passwordCryptographyProvider.encrypt(password, customerEntity.getSalt());
        if (!encryptedPassword.equals(customerEntity.getPassword())) {
            throw new AuthenticationFailedException("ATH-002", "Invalid Credentials");
        }

        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(encryptedPassword);
        CustomerAuthEntity customerAuthEntity = new CustomerAuthEntity();
        customerAuthEntity.setUuid(UUID.randomUUID().toString());
        customerAuthEntity.setCustomerEntity(customerEntity);
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime expiresAt = now.plusHours(8);
        customerAuthEntity.setAccessToken(
                jwtTokenProvider.generateToken(customerEntity.getUuid(), now, expiresAt));
        customerAuthEntity.setLoginAt(now);
        customerAuthEntity.setExpiresAt(expiresAt);

        customerAuthDao.createAuthToken(customerAuthEntity);
        customerDao.updateCustomerEntity(customerEntity);

        return customerAuthEntity;

    }





    private boolean isContactNumberAlreadyExists(final String contactNumber) {
        return customerDao.getCustomerByContactNumber(contactNumber)!= null;

    }


    private boolean isFieldOtherThanLastnameEmpty(final CustomerEntity customerEntity) {
        return  customerEntity.getFirstName().isEmpty() || customerEntity.getPassword().isEmpty() ||
                customerEntity.getContactNumber().isEmpty() || customerEntity.getEmail().isEmpty();
    }

    private boolean isEmailNotInCorrectFormat(final String email) {
        return false;
    }

    private boolean isContactNumberNotInCorrectFormat(final String contactNumber) {
        return false;
    }


    private boolean isPasswordWeak(final String password) {
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity logout(final String accessToken) throws AuthorizationFailedException {
        CustomerAuthEntity customerAuthEntity = customerAuthDao.getCustomerAuthByToken(accessToken);
        if (customerAuthEntity == null) {
            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in.");
        }

        if(customerAuthEntity.getLogoutAt()!=null){
            throw new AuthorizationFailedException("ATHR-002","Customer is logged out. Log in again to access this endpoint.");
        }

        if(customerAuthEntity.getExpiresAt().isBefore(ZonedDateTime.now())){
            throw new AuthorizationFailedException("ATHR-003","Your session is expired. Log in again to access this endpoint.");
        }
        customerAuthEntity.setLogoutAt(ZonedDateTime.now());
        customerAuthDao.updateCustomerAuth(customerAuthEntity);
        return customerAuthEntity.getCustomerEntity();

    }
}


