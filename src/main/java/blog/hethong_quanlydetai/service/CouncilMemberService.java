package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Council;
import blog.hethong_quanlydetai.entity.CouncilMember;
import blog.hethong_quanlydetai.repository.CouncilMemberRepository;
import blog.hethong_quanlydetai.repository.CouncilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CouncilMemberService {

    private final CouncilMemberRepository councilMemberRepository;
    private final CouncilRepository councilRepository;

    public CouncilMemberService(CouncilMemberRepository councilMemberRepository,
                                CouncilRepository councilRepository) {
        this.councilMemberRepository = councilMemberRepository;
        this.councilRepository = councilRepository;
    }

    public List<CouncilMember> findByCouncilId(Long councilId) {
        return councilMemberRepository.findByCouncilId(councilId);
    }

    public void addMember(Long councilId, Long lecturerId, String role) {
        if (councilMemberRepository.existsByCouncilIdAndLecturerId(councilId, lecturerId)) {
            throw new IllegalArgumentException("Giảng viên này đã có trong hội đồng.");
        }

        long totalMembers = councilMemberRepository.countByCouncilId(councilId);
        if (totalMembers >= 5) {
            throw new IllegalArgumentException("Hội đồng chỉ được tối đa 05 giảng viên.");
        }

        Council council = councilRepository.findById(councilId).orElseThrow();

        CouncilMember member = new CouncilMember();
        member.setCouncil(council);
        member.setLecturerId(lecturerId);
        member.setRole(role);

        councilMemberRepository.save(member);
    }

    public void delete(Long id) {
        councilMemberRepository.deleteById(id);
    }
}