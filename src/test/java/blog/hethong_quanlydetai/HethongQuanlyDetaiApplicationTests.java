package blog.hethong_quanlydetai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class HethongQuanlyDetaiApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SINH_VIEN", "NOTICE_VIEW"})
    void studentDashboardShowsRoleScopedInformationOnly() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không gian làm việc của bạn")))
                .andExpect(content().string(containsString("Thông báo mới")))
                .andExpect(content().string(not(containsString("student_groups"))));
    }

        @Test
        @WithMockUser(authorities = {"ROLE_GIANG_VIEN", "TOPIC_CREATE", "REGISTRATION_CONFIRM", "EVALUATION_CREATE"})
        void lecturerCannotOpenChairmanApprovalOrAssignmentScreens() throws Exception {
        mockMvc.perform(post("/topics/1/review").param("approved", "true").with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/review-assignments/save")
                .param("councilId", "1")
                .param("topicId", "1")
                .param("lecturerId", "1")
                .with(csrf()))
            .andExpect(status().isForbidden());
        }

    @Test
    @WithMockUser(authorities = "USER_MANAGE")
    void userFormExplainsPrimaryAndAdditionalRoles() throws Exception {
        mockMvc.perform(get("/users/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vai trò chính")))
                .andExpect(content().string(containsString("Nhóm người dùng")))
                .andExpect(content().string(containsString("quyền truy cập sẽ được cộng dồn")));
    }

    @Test
    @WithMockUser(authorities = "USER_MANAGE")
    void userListRendersPrimaryAndAdditionalRoleLabels() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nhóm quyền")))
                .andExpect(content().string(containsString("Danh sách tài khoản")));
    }

}
