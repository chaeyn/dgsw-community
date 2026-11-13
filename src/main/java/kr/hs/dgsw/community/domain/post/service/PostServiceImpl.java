package kr.hs.dgsw.community.domain.post.service;

import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.post.dto.request.PostCreateRequest;
import kr.hs.dgsw.community.domain.post.dto.response.PostPageResponse;
import kr.hs.dgsw.community.domain.post.dto.response.PostResponse;
import kr.hs.dgsw.community.domain.post.entity.Post;
import kr.hs.dgsw.community.domain.post.repository.PostRepository;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public PostResponse create(PostCreateRequest request) {
        Member author = memberRepository.findByIdAndStatus(request.authorId(), MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Post post = new Post(author, request.title(), request.content());
        return toResponse(postRepository.save(post));
    }

    @Override
    public PostResponse findById(Long id) {
        return toResponse(getPost(id));
    }

    @Override
    public PostPageResponse findAll(Pageable pageable) {
        PageRequest newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        return PostPageResponse.from(postRepository.findAll(newestFirst).map(PostServiceImpl::toResponse));
    }

    private Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private static PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
