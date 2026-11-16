package kr.hs.dgsw.community.domain.recruitment.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

import static kr.hs.dgsw.community.domain.member.entity.QMember.member;
import static kr.hs.dgsw.community.domain.recruitment.entity.QRecruitment.recruitment;

public class RecruitmentRepositoryImpl implements RecruitmentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public RecruitmentRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Recruitment> search(String keyword, RecruitmentPosition position,
                                    RecruitmentStatus status, Pageable pageable) {
        List<Recruitment> content = queryFactory
                .selectFrom(recruitment)
                .join(recruitment.author, member).fetchJoin()
                .where(titleContains(keyword), positionEq(position), statusEq(status))
                .orderBy(recruitment.createdAt.desc(), recruitment.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(recruitment.count())
                .from(recruitment)
                .where(titleContains(keyword), positionEq(position), statusEq(status))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    private BooleanExpression titleContains(String keyword) {
        return StringUtils.hasText(keyword) ? recruitment.title.containsIgnoreCase(keyword.trim()) : null;
    }

    private BooleanExpression positionEq(RecruitmentPosition position) {
        return position != null ? recruitment.position.eq(position) : null;
    }

    private BooleanExpression statusEq(RecruitmentStatus status) {
        return status != null ? recruitment.status.eq(status) : null;
    }
}
