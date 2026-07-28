package com.website.gis.core.mapper;

import com.website.gis.core.dto.LoginResponse;
import com.website.gis.core.dto.UserCreateRequest;
import com.website.gis.core.dto.UserDto;
import com.website.gis.core.dto.UserUpdateRequest;
import com.website.gis.core.entity.User;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Entity <-> DTO mapping for the User aggregate, used by AdminController
 * (CRUD on accounts) and AuthController (auth-facing user shape).
 *
 * See CODING_CONVENTIONS.md Section 3.3 for what is deliberately NOT
 * mapped here (conditional password re-hash on update, role override
 * from Authentication on /me) — that logic stays in the controllers.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    LoginResponse toLoginResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "VIEWER") // Only VIEWER accounts can be created via this admin endpoint
    @Mapping(target = "password", ignore = true) // Hashed by the caller using PasswordEncoder
    User toEntity(UserCreateRequest request);

    /**
     * Partial update: only fullName is copied here. password is
     * intentionally ignored — AdminController#updateUser only re-hashes
     * and sets it when the request actually supplies a non-blank value,
     * which is a conditional business rule, not a pure field mapping.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntityFromRequest(UserUpdateRequest request, @MappingTarget User user);
}
