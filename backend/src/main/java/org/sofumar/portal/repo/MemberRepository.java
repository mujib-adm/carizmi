package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.MemberVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<MemberVO, Integer>, JpaSpecificationExecutor<MemberVO> {
}

//public interface MemberRepository extends JpaRepository<MemberVO, Integer> {

//    Page<Member> findByStatus(String status, Pageable pageable);
//
//    @Query("select m from Member m where concat(m.firstName,' ',m.lastName,m.phone) like %:q%")
//    Page<Member> search(@Param("q") String q, Pageable pageable);
//
//    List<Member> findByStatus(String status);
//
//    List<Member> findByCityContainingIgnoreCase(String city);
//
//    List<Member> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrPhoneContaining(
//            String firstName, String lastName, String phone
//    );
//}