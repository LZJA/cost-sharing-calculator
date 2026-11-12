package com.costsharing.calculator.auth;

import com.costsharing.calculator.service.CalculatorService;
import com.costsharing.calculator.repository.CalculatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class CompanionService {
    private final CalculatorService calculators;
    private final CalculatorRepository calculatorRepository;
    private final CompanionRepository companions;
    private final InvitationRepository invitations;
    private final AccountRepository accounts;
    public record Person(Long accountId, String displayName, String role) {}
    public record Group(List<Person> people, List<Invitation> invitations, String currentRole) {}
    public record InviteLink(Long id, String token, Instant expiresAt) {}
    public record InvitePreview(String calculatorName, String ownerName, boolean alreadyJoined) {}

    public Group list(Long id) {
        var calculator = calculators.getById(id);
        List<Person> people = new ArrayList<>();
        if (calculator.getOwnerId()!=null) accounts.findById(calculator.getOwnerId()).ifPresent(a -> people.add(new Person(a.getId(), a.getDisplayName(), "OWNER")));
        var members = companions.findByCalculatorId(id);
        var names = new HashMap<Long,String>();
        accounts.findAllById(members.stream().map(Companion::getAccountId).toList()).forEach(a -> names.put(a.getId(), a.getDisplayName()));
        members.forEach(m -> people.add(new Person(m.getAccountId(), names.getOrDefault(m.getAccountId(), "微信用户"), "MEMBER")));
        boolean owner = "OWNER".equals(calculator.getCurrentRole());
        return new Group(people, owner ? invitations.findByCalculatorIdOrderByIdDesc(id).stream()
                .filter(i -> !i.isRevoked() && i.getAcceptedBy()==null && i.getExpiresAt().isAfter(Instant.now())).toList() : List.of(), calculator.getCurrentRole());
    }
    @Transactional public InviteLink invite(Long id) {
        lockCalculator(id); calculators.requireOwner(id);
        String token=Tokens.create();
        var invite=new Invitation(); invite.setCalculatorId(id); invite.setTokenHash(Tokens.hash(token));
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS)); invitations.save(invite);
        return new InviteLink(invite.getId(), token, invite.getExpiresAt());
    }
    public InvitePreview preview(String token) {
        CurrentAccount.id();
        var invite = find(token);
        if (!returningMember(invite)) validate(invite);
        var calculator = calculatorRepository.findById(invite.getCalculatorId()).orElseThrow(this::invalid);
        String owner = calculator.getOwnerId()==null ? "创建者" : accounts.findById(calculator.getOwnerId()).map(Account::getDisplayName).orElse("创建者");
        boolean joined=Objects.equals(calculator.getOwnerId(), CurrentAccount.id()) || companions.existsByCalculatorIdAndAccountId(calculator.getId(), CurrentAccount.id());
        return new InvitePreview(calculator.getName(), owner, joined);
    }
    @Transactional public Long accept(String token) {
        if (token==null || token.length()!=43) throw invalid();
        Long id=invitations.findCalculatorIdByHash(Tokens.hash(token)).orElseThrow(this::invalid);
        lockCalculator(id);
        var invite = invitations.lockByHash(Tokens.hash(token)).orElseThrow(this::invalid);
        if (returningMember(invite)) return invite.getCalculatorId();
        validate(invite);
        var calculator=calculatorRepository.findById(invite.getCalculatorId()).orElseThrow(this::invalid);
        if (Objects.equals(calculator.getOwnerId(), CurrentAccount.id()) || companions.existsByCalculatorIdAndAccountId(calculator.getId(), CurrentAccount.id()))
            return calculator.getId(); // 已是成员不消耗留给其他同伴的邀请。
        var member=new Companion(); member.setCalculatorId(calculator.getId()); member.setAccountId(CurrentAccount.id());
        companions.save(member); invite.setAcceptedBy(CurrentAccount.id()); invitations.save(invite);
        return calculator.getId();
    }
    @Transactional public void revoke(Long id, Long inviteId) {
        lockCalculator(id); calculators.requireOwner(id);
        var invite=invitations.findById(inviteId).filter(i -> i.getCalculatorId().equals(id)).orElseThrow(this::invalid);
        invite.setRevoked(true); invitations.save(invite);
    }
    @Transactional public void remove(Long id, Long accountId) {
        lockCalculator(id); var calculator=calculators.requireOwner(id);
        if (Objects.equals(calculator.getOwnerId(), accountId)) throw new AccessException(400, "不能移除创建者");
        companions.deleteByCalculatorIdAndAccountId(id, accountId);
        // 防止被移除的人通过已拿到的旧邀请再次加入。创建者可重新邀请。
        invitations.revokeAll(id);
    }
    private Invitation find(String token) {
        if (token==null || token.length()!=43) throw invalid();
        return invitations.findByTokenHash(Tokens.hash(token)).orElseThrow(this::invalid);
    }
    private boolean returningMember(Invitation invite) {
        return !invite.isRevoked() && Objects.equals(invite.getAcceptedBy(), CurrentAccount.id()) &&
                companions.existsByCalculatorIdAndAccountId(invite.getCalculatorId(), CurrentAccount.id());
    }
    private void validate(Invitation invitation) {
        if (invitation.isRevoked() || invitation.getAcceptedBy()!=null || !invitation.getExpiresAt().isAfter(Instant.now())) throw invalid();
    }
    private AccessException invalid() { return new AccessException(410, "邀请已失效，请联系创建者重新邀请"); }
    private void lockCalculator(Long id) { calculatorRepository.lockById(id).orElseThrow(() -> new AccessException(404, "计算器不存在或无权访问")); }
}
