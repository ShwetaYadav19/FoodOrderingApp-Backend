package com.upgrad.FoodOrderingApp.api.controller;


import java.util.Base64;
import com.upgrad.FoodOrderingApp.api.model.LoginResponse;
import com.upgrad.FoodOrderingApp.api.model.LogoutResponse;
import com.upgrad.FoodOrderingApp.api.model.SignupCustomerRequest;
import com.upgrad.FoodOrderingApp.api.model.SignupCustomerResponse;
import com.upgrad.FoodOrderingApp.api.model.UpdateCustomerRequest;
import com.upgrad.FoodOrderingApp.api.model.UpdateCustomerResponse;
import com.upgrad.FoodOrderingApp.service.businness.CommonCustomerService;
import com.upgrad.FoodOrderingApp.service.businness.CustomerAuthenticationService;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import com.upgrad.FoodOrderingApp.service.exception.UpdateCustomerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin
@RestController
@RequestMapping("/")
public class CustomerController {

    @Autowired
    private CustomerAuthenticationService customerAuthenticationService;

    @Autowired
    private CommonCustomerService commonCustomerService;

    @RequestMapping(method = RequestMethod.POST, path = "/customer/signup", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SignupCustomerResponse> signupCustomer(final SignupCustomerRequest signupUserRequest)
            throws SignUpRestrictedException {

        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setFirstName(signupUserRequest.getFirstName());
        customerEntity.setLastName(signupUserRequest.getLastName());
        customerEntity.setContactNumber(signupUserRequest.getContactNumber());
        customerEntity.setEmail(signupUserRequest.getEmailAddress());
        customerEntity.setPassword(signupUserRequest.getPassword());

        CustomerEntity createdCustomerEntity = customerAuthenticationService.signup(customerEntity);
        SignupCustomerResponse signupCustomerResponse = new SignupCustomerResponse()
                .id(createdCustomerEntity.getUuid())
                .status("CUSTOMER SUCCESSFULLY REGISTERED");

        return new ResponseEntity<SignupCustomerResponse>(signupCustomerResponse, HttpStatus.CREATED);

    }


    @RequestMapping(method = RequestMethod.POST, path = "/customer/login", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LoginResponse> loginCustomer(@RequestHeader("authorization") final String authorization)
            throws AuthenticationFailedException {

        byte[] decode = Base64.getDecoder().decode(authorization.split("Basic ")[1]);
        String decodedText = new String(decode);
        String[] decodedArray = decodedText.split(":");
        CustomerAuthEntity customerAuthEntity = customerAuthenticationService.login(decodedArray[0], decodedArray[1]);

        HttpHeaders headers = new HttpHeaders();
        headers.add("access-token", customerAuthEntity.getAccessToken());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(customerAuthEntity.getCustomerEntity().getUuid());
        loginResponse.setFirstName(customerAuthEntity.getCustomerEntity().getFirstName());
        loginResponse.setLastName(customerAuthEntity.getCustomerEntity().getLastName());
        loginResponse.setEmailAddress(customerAuthEntity.getCustomerEntity().getEmail());
        loginResponse.setContactNumber(customerAuthEntity.getCustomerEntity().getContactNumber());
        loginResponse.setMessage("LOGGED IN SUCCESSFULLY");

        return new ResponseEntity<LoginResponse>(loginResponse, headers, HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.POST, path = "/customer/logout", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LogoutResponse> logoutCustomer(@RequestHeader("authorization") final String accessToken)
            throws AuthorizationFailedException {
        CustomerEntity customerEntity = customerAuthenticationService.logout(accessToken);
        LogoutResponse logoutResponse =
                new LogoutResponse().id(customerEntity.getUuid()).message("LOGGED OUT SUCCESSFULLY");
        return new ResponseEntity<LogoutResponse>(logoutResponse, HttpStatus.OK);

    }

    @RequestMapping(method = RequestMethod.POST, path = "/customer", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<UpdateCustomerResponse> logoutCustomer(@RequestHeader("authorization") final String bearerToken,
                                                                 final UpdateCustomerRequest updateCustomerRequest)
            throws UpdateCustomerException, AuthorizationFailedException {

        String accessToken = bearerToken.split("Bearer ")[1];

        CustomerEntity updatedCustomerEntity = commonCustomerService.updateCustomer(accessToken,updateCustomerRequest.getFirstName(),
                updateCustomerRequest.getLastName());

        UpdateCustomerResponse updateCustomerResponse =
                new UpdateCustomerResponse().id(updatedCustomerEntity.getUuid()).
                        status("CUSTOMER DETAILS UPDATED SUCCESSFULLY")
                .firstName(updatedCustomerEntity.getFirstName())
                .lastName(updatedCustomerEntity.getLastName());
        return new ResponseEntity<UpdateCustomerResponse>(updateCustomerResponse, HttpStatus.OK);

    }


}