package com.example.diplomproject.controller;

import com.example.diplomproject.dto.CourseDto;
import com.example.diplomproject.dto.ReviewDto;
import com.example.diplomproject.entity.Category;
import com.example.diplomproject.entity.Certificate;
import com.example.diplomproject.entity.Course;
import com.example.diplomproject.entity.User;
import com.example.diplomproject.mapper.CategoryMapper;
import com.example.diplomproject.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;


@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    private final ReviewService reviewService;
    private final CourseAccessService courseAccessService;
    private final CertificateService certificateService;

    @Autowired
    public CourseController(CourseService courseService,
                            CategoryService categoryService,
                            CategoryMapper categoryMapper,
                            ReviewService reviewService,
                            CourseAccessService courseAccessService,
                            CertificateService certificateService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
        this.reviewService = reviewService;
        this.courseAccessService = courseAccessService;
        this.certificateService = certificateService;
    }

    @GetMapping
    public String listCourses(@RequestParam(value = "categoryId", required = false) Long categoryId,
                              Model model) {
        List<CourseDto> courses;
        if (categoryId != null) {
            Category category = null;
            try {
                category = categoryService.getCategoryById(categoryId);
            } catch (IllegalArgumentException e) {
                // категория не найдена
            }
            if (category != null) {
                model.addAttribute("category", categoryMapper.toCategoryDTO(category));
                courses = courseService.getCourseDtosByCategoryId(categoryId);
            } else {
                courses = Collections.emptyList();
            }
        } else {
            courses = courseService.getAllCourses();
        }
        model.addAttribute("courses", courses);
        model.addAttribute("title", "Курсы");
        model.addAttribute("content", "pages/courses/courses :: user-courses-content");
        return "layouts/main";
    }

    @GetMapping("/{id}")
    public String viewCourse(@PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             Model model) {
        CourseDto courseDto;
        try {
            courseDto = courseService.getCourseDtoById(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Курс не найден");
        }
        // Получаем отзывы с учётом прав пользователя (сервис сам фильтрует)
        List<ReviewDto> visibleReviews = reviewService.getVisibleReviewDtosByCourseId(id, currentUser);
        double averageRating = reviewService.averageRatingForCourse(id);
        int reviewCount = reviewService.getApprovedReviewCount(id);

        boolean purchased = false;
        boolean hasCertificate = false;
        boolean hasMaterials = false;
        boolean canReview = false;

        if (currentUser != null) {
            purchased = courseAccessService.hasAccessToUserForDto(currentUser, courseDto);
            if (purchased) {
                Certificate cert = certificateService.findByUserAndCourse(currentUser.getId(), id);
                hasCertificate = (cert != null && !cert.isRevoked() && cert.getCertificateUrl() != null);
                hasMaterials = (courseDto.getMaterialsPath() != null && !courseDto.getMaterialsPath().isEmpty());

                // Проверяем, есть ли у пользователя активный отзыв (PENDING или APPROVED)
                canReview = !reviewService.hasUserActiveReview(currentUser.getId(), id);
            }
        }

        model.addAttribute("course", courseDto);
        model.addAttribute("purchased", purchased);
        model.addAttribute("hasCertificate", hasCertificate);
        model.addAttribute("hasMaterials", hasMaterials);
        model.addAttribute("title", courseDto.getTitle());
        model.addAttribute("content", "pages/courses/detail :: detail-content");
        model.addAttribute("reviews", visibleReviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("canReview", canReview);

        return "layouts/main";
    }

//    private boolean isAdmin(User currentUser) {
//        return currentUser != null && currentUser.getRole() == Role.ADMIN;
//    }

    @GetMapping("/search")
    public String searchCourses(@RequestParam(required = false) String title, Model model) {
        List<CourseDto> courses = courseService.searchCourseDtosByTitle(title);
        model.addAttribute("courses", courses);
        model.addAttribute("searchTitle", title);
        model.addAttribute("title", title != null && !title.isEmpty()
                ? "Результаты поиска: " + title
                : "Все курсы");
        model.addAttribute("content", "pages/courses/courses :: user-courses-content");
        return "layouts/main";
    }

    @GetMapping("/{id}/download-materials")
    public ResponseEntity<Resource> downloadCourseMaterials(@PathVariable Long id,
                                                            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Необходимо авторизоваться");
        }
        Course course;
        try {
            course = courseService.getCourseEntityById(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Курс не найден");
        }
        if (!courseAccessService.hasAccessToUser(user, course)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Курс не оплачен");
        }
        if (course.getMaterialsPath() == null || course.getMaterialsPath().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Материалы не загружены");
        }
        try {
            Path filePath = Paths.get(course.getMaterialsPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String encodedFilename = URLEncoder.encode(course.getTitle() + ".zip", StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20");
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/zip"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename*=UTF-8''" + encodedFilename)
                        .body(resource);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Файл материалов не найден");
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка формирования пути к файлу");
        }
    }
}