package kr.hs.dgsw.community.domain.recruitment.repository;

import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecruitmentRepositoryCustom {

    Page<Recruitment> search(String keyword, RecruitmentPosition position,
                             RecruitmentStatus status, Pageable pageable);
}
