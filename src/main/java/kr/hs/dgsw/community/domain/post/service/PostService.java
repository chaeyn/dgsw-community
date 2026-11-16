package kr.hs.dgsw.community.domain.post.service;

import kr.hs.dgsw.community.domain.post.dto.request.PostCreateRequest;
import kr.hs.dgsw.community.domain.post.dto.request.PostUpdateRequest;
import kr.hs.dgsw.community.domain.post.dto.response.PostPageResponse;
import kr.hs.dgsw.community.domain.post.dto.response.PostResponse;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponse create(PostCreateRequest request);

    PostResponse findById(Long id);

    PostPageResponse findAll(Pageable pageable);

    PostResponse update(Long id, PostUpdateRequest request);
}
