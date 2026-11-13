package kr.hs.dgsw.community.domain.post.service;

import kr.hs.dgsw.community.domain.post.dto.request.PostCreateRequest;
import kr.hs.dgsw.community.domain.post.dto.response.PostResponse;

public interface PostService {

    PostResponse create(PostCreateRequest request);
}
