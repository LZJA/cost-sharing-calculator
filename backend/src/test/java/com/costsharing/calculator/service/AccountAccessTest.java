package com.costsharing.calculator.service;

import com.costsharing.calculator.auth.*;
import com.costsharing.calculator.dto.CalculatorRequest;
import com.costsharing.calculator.formula.DefaultConfigs;
import com.costsharing.calculator.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:accountaccess;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=year,month",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "logging.level.org.hibernate.SQL=WARN", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc @Transactional
class AccountAccessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired InvitationRepository invitations;
    @Autowired LoginSessionRepository sessions;
    @Autowired CalculatorRepository calculators;
    @Autowired BillRepository bills;
    @MockBean WechatIdentity wechat;
    String owner, member, stranger, ownerCode;
    long calculatorId;

    @BeforeEach void setup() throws Exception {
        when(wechat.exchange(anyString())).thenAnswer(i -> "openid-" + i.getArgument(0));
        String suffix=java.util.UUID.randomUUID().toString();
        ownerCode="owner-"+suffix;
        owner=login(ownerCode); member=login("member-"+suffix); stranger=login("stranger-"+suffix);
        var request=new CalculatorRequest(); request.setName("私人的分账"); request.setConfig(DefaultConfigs.gezi());
        calculatorId=data(mvc.perform(post("/calculators").header("Authorization",owner).contentType("application/json")
                .content(json.writeValueAsString(request))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    String login(String code) throws Exception {
        return "Bearer " + data(mvc.perform(post("/auth/login").contentType("application/json").content(json.writeValueAsString(java.util.Map.of("code",code))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }
    JsonNode data(String body) throws Exception { return json.readTree(body).get("data"); }
    JsonNode invite() throws Exception {
        return data(mvc.perform(post("/calculators/"+calculatorId+"/invitations").header("Authorization",owner))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    void join(String token) throws Exception {
        mvc.perform(post("/invitations/"+token+"/accept").header("Authorization",member)).andExpect(status().isOk());
    }
    String billPayload() { return "{\"calculatorId\":"+calculatorId+",\"year\":2026,\"month\":11,\"ruleName\":\"普通分账\",\"fees\":{\"water\":100,\"electric\":200,\"gas\":30}}"; }
    @Test void unauthenticatedAndSpoofedIdentityCannotAccessAnyProtectedData() throws Exception {
        mvc.perform(get("/calculators")).andExpect(status().isUnauthorized());
        mvc.perform(get("/calculators").header("X-WX-OPENID","openid-owner")).andExpect(status().isUnauthorized());
        mvc.perform(get("/calculators").header("Authorization","Bearer forged")).andExpect(status().isUnauthorized());
    }
    @Test void unrelatedAccountsCannotListReadWriteOrDeleteOthersCalculatorsAndBills() throws Exception {
        mvc.perform(get("/calculators").header("Authorization",stranger)).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/calculators/"+calculatorId).header("Authorization",stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/bills").param("calculatorId",String.valueOf(calculatorId)).header("Authorization",stranger)).andExpect(status().isNotFound());
        mvc.perform(post("/bills").header("Authorization",stranger).contentType("application/json").content(billPayload())).andExpect(status().isNotFound());
        mvc.perform(delete("/calculators/"+calculatorId).header("Authorization",stranger)).andExpect(status().isNotFound());
        var request=new CalculatorRequest(); request.setName("试图修改"); request.setConfig(DefaultConfigs.gezi());
        mvc.perform(put("/calculators/"+calculatorId).header("Authorization",stranger).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isNotFound());
    }
    @Test void invitedMemberCanRecordAndViewButOnlyOwnerCanManageOrDelete() throws Exception {
        String token=invite().get("token").asText();
        mvc.perform(get("/invitations/"+token).header("Authorization",member)).andExpect(jsonPath("$.data.calculatorName").value("私人的分账"));
        // 打开邀请不会自动授权。
        mvc.perform(get("/calculators/"+calculatorId).header("Authorization",member)).andExpect(status().isNotFound());
        join(token);
        mvc.perform(get("/calculators").header("Authorization",member)).andExpect(jsonPath("$.data[0].currentRole").value("MEMBER"));
        mvc.perform(get("/calculators/"+calculatorId).header("Authorization",member)).andExpect(status().isOk());
        long billId=data(mvc.perform(post("/bills").header("Authorization",member).contentType("application/json").content(billPayload()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").doesNotExist()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/bills").param("calculatorId",String.valueOf(calculatorId)).header("Authorization",member)).andExpect(jsonPath("$.data.content[0].canDelete").value(false));
        var request=new CalculatorRequest(); request.setName("同伴修改"); request.setConfig(DefaultConfigs.gezi());
        mvc.perform(put("/calculators/"+calculatorId).header("Authorization",member).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isForbidden());
        mvc.perform(post("/calculators/"+calculatorId+"/invitations").header("Authorization",member)).andExpect(status().isForbidden());
        mvc.perform(delete("/bills/"+billId).header("Authorization",member)).andExpect(status().isForbidden());
        mvc.perform(delete("/bills/"+billId).header("Authorization",stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/bills").param("calculatorId",String.valueOf(calculatorId)).header("Authorization",owner)).andExpect(jsonPath("$.data.content[0].canDelete").value(true));
        mvc.perform(delete("/bills/"+billId).header("Authorization",owner)).andExpect(status().isOk());
    }
    @Test void removalRevokesAccessAndOldInvitesButPreservesBillsAndAllowsFreshInvite() throws Exception {
        String used=invite().get("token").asText(); join(used);
        String spare=invite().get("token").asText();
        mvc.perform(post("/bills").header("Authorization",member).contentType("application/json").content(billPayload())).andExpect(status().isOk());
        long memberId=data(mvc.perform(get("/auth/me").header("Authorization",member)).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/calculators/"+calculatorId+"/companions/"+memberId).header("Authorization",member)).andExpect(status().isForbidden());
        mvc.perform(delete("/calculators/"+calculatorId+"/companions/"+memberId).header("Authorization",owner)).andExpect(status().isOk());
        mvc.perform(get("/calculators/"+calculatorId).header("Authorization",member)).andExpect(status().isNotFound());
        mvc.perform(post("/bills").header("Authorization",member).contentType("application/json").content(billPayload())).andExpect(status().isNotFound());
        for (String token : new String[]{used,spare}) mvc.perform(post("/invitations/"+token+"/accept").header("Authorization",member)).andExpect(status().isGone());
        assertEquals(1,bills.countByCalculatorId(calculatorId));
        join(invite().get("token").asText());
        mvc.perform(get("/calculators/"+calculatorId).header("Authorization",member)).andExpect(status().isOk());
    }
    @Test void expiredRevokedAndConsumedInvitesCannotGrantAccess() throws Exception {
        var revoked=invite();
        mvc.perform(delete("/calculators/"+calculatorId+"/invitations/"+revoked.get("id").asLong()).header("Authorization",stranger)).andExpect(status().isNotFound());
        mvc.perform(delete("/calculators/"+calculatorId+"/invitations/"+revoked.get("id").asLong()).header("Authorization",owner)).andExpect(status().isOk());
        mvc.perform(post("/invitations/"+revoked.get("token").asText()+"/accept").header("Authorization",member)).andExpect(status().isGone());
        var expired=invite(); var entity=invitations.findById(expired.get("id").asLong()).orElseThrow(); entity.setExpiresAt(Instant.now().minusSeconds(1)); invitations.saveAndFlush(entity);
        mvc.perform(get("/invitations/"+expired.get("token").asText()).header("Authorization",member)).andExpect(status().isGone());
        String used=invite().get("token").asText(); join(used);
        mvc.perform(post("/invitations/"+used+"/accept").header("Authorization",stranger)).andExpect(status().isGone());
    }
    @Test void sessionsExpireAndLogoutRevokesTokenAndNeverExposeOpenidToCompanions() throws Exception {
        mvc.perform(get("/auth/me").header("Authorization",owner)).andExpect(jsonPath("$.data.openid").doesNotExist());
        var session=sessions.findById(Tokens.hash(owner.substring(7))).orElseThrow(); session.setExpiresAt(Instant.now().minusSeconds(1)); sessions.saveAndFlush(session);
        mvc.perform(get("/calculators").header("Authorization",owner)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/auth/session").header("Authorization",member)).andExpect(status().isOk());
        mvc.perform(get("/calculators").header("Authorization",member)).andExpect(status().isUnauthorized());
    }
    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentAcceptsCannotConsumeOneInvitationTwice() throws Exception {
        String token=invite().get("token").asText();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            var first=pool.submit(() -> { start.await(); return mvc.perform(post("/invitations/"+token+"/accept").header("Authorization",member)).andReturn().getResponse().getStatus(); });
            var second=pool.submit(() -> { start.await(); return mvc.perform(post("/invitations/"+token+"/accept").header("Authorization",stranger)).andReturn().getResponse().getStatus(); });
            start.countDown();
            var statuses=new java.util.ArrayList<>(java.util.List.of(first.get(10,java.util.concurrent.TimeUnit.SECONDS),second.get(10,java.util.concurrent.TimeUnit.SECONDS)));
            statuses.sort(Integer::compareTo);
            assertEquals(java.util.List.of(200,410),statuses);
            mvc.perform(get("/calculators/"+calculatorId+"/companions").header("Authorization",owner)).andExpect(jsonPath("$.data.people.length()").value(2));
        } finally { pool.shutdownNow(); }
    }
}
