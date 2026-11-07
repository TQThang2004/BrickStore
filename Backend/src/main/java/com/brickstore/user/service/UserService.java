package com.brickstore.user.service;

import com.brickstore.user.dto.request.ChangePasswordRequest;
import com.brickstore.user.dto.request.UpdateProfileRequest;
import com.brickstore.user.dto.response.UserResponse;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    UserResponse getUserById(Long id);
}

