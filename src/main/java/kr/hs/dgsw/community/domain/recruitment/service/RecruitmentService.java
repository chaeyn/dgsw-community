package kr.hs.dgsw.community.domain.recruitment.service;

import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentUpdateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentPageResponse;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentResponse;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import org.springframework.data.domain.Pageable;

public interface RecruitmentService {

    RecruitmentResponse create(Long memberId, RecruitmentCreateRequest request);

    RecruitmentResponse findById(Long id);

    RecruitmentPageResponse search(String keyword, RecruitmentPosition position,
                                   RecruitmentStatus status, Pageable pageable);

    RecruitmentResponse update(Long id, Long memberId, RecruitmentUpdateRequest request);

    RecruitmentResponse close(Long id, Long memberId);

    void delete(Long id, Long memberId);
}
