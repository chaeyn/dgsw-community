package kr.hs.dgsw.community.domain.post.repository;

import kr.hs.dgsw.community.domain.post.entity.Post;
import kr.hs.dgsw.community.domain.post.dto.response.PostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(value = """
            select new kr.hs.dgsw.community.domain.post.dto.response.PostListResponse(
                p.id, p.author.id, p.title, p.createdAt, p.updatedAt)
            from Post p
            """, countQuery = "select count(p) from Post p")
    Page<PostListResponse> findPostSummaries(Pageable pageable);
}
