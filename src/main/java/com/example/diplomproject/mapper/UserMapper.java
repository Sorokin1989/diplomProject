package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.UserDto;
import com.example.diplomproject.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CertificateMapper certificateMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private CourseAccessMapper courseAccessMapper;

    public UserDto toUserDto(User user) {

        if (user == null) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setId(user.getId());

        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setRole(String.valueOf(user.getRole()));
        userDto.setRegistrationDate(user.getRegistrationDate());

        if (user.getOrders() != null) {
            userDto.setOrderDtos(user.getOrders().stream().
                    map(order -> orderMapper.toOrderDTO(order)).toList());
        }

        if (user.getCertificates() != null) {
            userDto.setCertificateDtos(user.getCertificates().stream().
                    map(certificate -> certificateMapper.toCertificateDto(certificate)).toList());
        }

        if (user.getCart() != null) {
            userDto.setCartId(user.getCart().getId());
        } else {
            userDto.setCartId(null);
        }

        if (user.getReviews() != null) {
            userDto.setReviewDtos(user.getReviews().stream().
                    map(review -> reviewMapper.toReviewDTO(review)).toList());
        }

        if (user.getCourseAccesses() != null) {
            userDto.setCourseAccessDtos(user.getCourseAccesses().stream().
                    map(courseaccess -> courseAccessMapper.toCourseAccessDto(courseaccess)).toList());
        }

        userDto.setBonusPoints(user.getBonusPoints());
        return userDto;

    }
}
