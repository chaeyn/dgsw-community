package kr.hs.dgsw.community.domain.post.controller;

import jakarta.validation.Valid;
import kr.hs.dgsw.community.domain.post.dto.request.PostCreateRequest;
import kr.hs.dgsw.community.domain.post.dto.response.PostPageResponse;
import kr.hs.dgsw.community.domain.post.dto.response.PostResponse;
import kr.hs.dgsw.community.domain.post.service.PostService;
import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.global.response.ApiResponse;
import kr.hs.dgsw.community.global.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(@Valid @RequestBody PostCreateRequest request) {
        PostResponse response = postService.create(request);
        return ResponseEntity.created(URI.create("/api/posts/" + response.id()))
                .body(ApiResponse.of("게시글이 등록되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PostPageResponse>> findAll(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.of("게시글 목록 조회에 성공했습니다.", postService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("게시글 조회에 성공했습니다.", postService.findById(id)));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput() {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE));
    }
}
