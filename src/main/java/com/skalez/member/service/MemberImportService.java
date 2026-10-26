package com.skalez.member.service;

import com.skalez.member.api.AdminMemberDtos.ImportMemberRow;
import com.skalez.member.api.AdminMemberDtos.ImportMembersRequest;
import com.skalez.member.api.AdminMemberDtos.ImportResult;
import com.skalez.member.api.AdminMemberDtos.ImportResult.Failure;
import com.skalez.member.domain.Member;
import com.skalez.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 아임웹 회원 엑셀(CSV)을 관리자 화면에서 읽어 보낸 행들을 회원으로 만든다.
 * - 비밀번호는 아임웹에서 가져올 수 없어서 비워 둔다 → 처음 접속 때 '비밀번호 찾기' 메일로 설정
 * - 이미 있는 이메일은 건너뛴다 (여러 번 올려도 중복되지 않는다)
 * - 행 하나가 잘못돼도 나머지는 들어가고, 잘못된 행은 결과로 돌려준다
 */
@Service
@RequiredArgsConstructor
public class MemberImportService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final MemberRepository memberRepository;

    @Transactional
    public ImportResult importMembers(ImportMembersRequest request) {
        int created = 0;
        int skipped = 0;
        List<Failure> failures = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        List<ImportMemberRow> rows = request.members();
        for (int i = 0; i < rows.size(); i++) {
            ImportMemberRow row = rows.get(i);
            String email = row.email() == null ? "" : row.email().trim().toLowerCase(Locale.ROOT);
            if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
                failures.add(new Failure(i, row.email(), "이메일 형식이 아니에요"));
                continue;
            }
            if (!seen.add(email) || memberRepository.existsByEmail(email)) {
                skipped++;
                continue;
            }
            String name = row.name() == null || row.name().isBlank() ? email.substring(0, email.indexOf('@')) : row.name().trim();
            String phone = row.phone() == null || row.phone().isBlank() ? null : row.phone().trim();
            memberRepository.save(Member.importLegacy(Member.LEGACY_IMWEB, blankToNull(row.legacyId()), email, truncate(name, 50), phone,
                    row.joinedAt(), Boolean.TRUE.equals(row.marketingConsent()), row.marketingConsentAt()));
            created++;
        }
        return new ImportResult(created, skipped, failures);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : truncate(value.trim(), 50);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
