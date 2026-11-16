package kr.hs.dgsw.community.domain.recruitment.service;

import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationDecisionRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentApplicationResponse;

import java.util.List;

public interface RecruitmentApplicationService {

    RecruitmentApplicationResponse apply(Long recruitmentId, RecruitmentApplicationCreateRequest request);

    List<RecruitmentApplicationResponse> findAll(Long recruitmentId, Long memberId);

    RecruitmentApplicationResponse decide(Long recruitmentId, Long applicationId,
                                          RecruitmentApplicationDecisionRequest request);

    void cancel(Long recruitmentId, Long applicationId, Long memberId);
}
