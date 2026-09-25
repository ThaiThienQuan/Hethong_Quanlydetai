package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Council;
import blog.hethong_quanlydetai.repository.CouncilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import blog.hethong_quanlydetai.repository.CouncilMemberRepository;

import java.util.List;

@Service
@Transactional
public class CouncilService {

    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository councilMemberRepository;

    public CouncilService(CouncilRepository councilRepository,
                      CouncilMemberRepository councilMemberRepository) {
    this.councilRepository = councilRepository;
    this.councilMemberRepository = councilMemberRepository;
}

    public List<Council> findAll() {
        return councilRepository.findAllByOrderByReportDateDesc();
    }

    public Council findById(Long id) {
        return councilRepository.findById(id).orElseThrow();
    }

    public Council save(Council council) {
        if (council.getStatus() == null || council.getStatus().isBlank()) {
            council.setStatus("PLANNED");
        }

        return councilRepository.save(council);
    }

    public void delete(Long id) {
        councilRepository.deleteById(id);
    }
    public void completeCouncil(Long councilId) {
    Council council = findById(councilId);

    long totalMembers = councilMemberRepository.countByCouncilId(councilId);
    long chairmanCount = councilMemberRepository.countByCouncilIdAndRole(councilId, "CHAIRMAN");
    long secretaryCount = councilMemberRepository.countByCouncilIdAndRole(councilId, "SECRETARY");

    if (totalMembers < 3 || totalMembers > 5) {
        throw new IllegalArgumentException("Hội đồng phải có từ 03 đến 05 giảng viên.");
    }

    if (chairmanCount != 1) {
        throw new IllegalArgumentException("Hội đồng phải có đúng 01 chủ tịch.");
    }

    if (secretaryCount != 1) {
        throw new IllegalArgumentException("Hội đồng phải có đúng 01 thư ký.");
    }

    council.setStatus("COMPLETED");
    councilRepository.save(council);
}
}