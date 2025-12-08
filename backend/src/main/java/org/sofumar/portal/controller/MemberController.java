package org.sofumar.portal.controller;

import org.sofumar.portal.data.vo.MemberVO;
import org.sofumar.portal.repo.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {
    private final MemberRepository repo;

    public MemberController(MemberRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<MemberVO> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{memberID}")
    public ResponseEntity<MemberVO> getByMemberID(@PathVariable Integer memberID) {
        return repo.findById(memberID).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public MemberVO create(@Validated @RequestBody MemberVO memberVO) {
        return repo.save(memberVO);
    }

    @PutMapping("/{memberID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<MemberVO> update(@PathVariable Integer memberID, @Validated @RequestBody MemberVO updated) {
        return repo.findById(memberID).map(existing -> {
            updated.setMemberID(memberID);
            return ResponseEntity.ok(repo.save(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{memberID}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer memberID) {
        if (!repo.existsById(memberID)) return ResponseEntity.notFound().build();
        repo.deleteById(memberID);
        return ResponseEntity.noContent().build();
    }

/*
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Page<Member> list(@RequestParam Optional<String> q, Pageable pageable) {
        return q.filter(s -> !s.isBlank()).map(s -> repo.search(s, pageable)).orElse(repo.findAll(pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Member create(@Valid @RequestBody Member m, Authentication auth) {
//        m.setCreateUserID(auth.getName());
        return repo.save(m);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Member update(@PathVariable Integer id, @Valid @RequestBody Member m, Authentication auth) {
        Member existing = repo.findById(id).orElseThrow();
        BeanUtils.copyProperties(m, existing, "id","createUserID","createDateTime");
//        existing.setChangeUserID(auth.getName());
        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(@PathVariable Integer id) {
        repo.deleteById(id);
    }
*/
}
