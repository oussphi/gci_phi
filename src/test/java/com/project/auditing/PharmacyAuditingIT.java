package com.project.auditing;

import com.project.entity.Pharmacy;
import com.project.repository.AuditLogRepository;
import com.project.repository.PharmacyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PharmacyAuditingIT {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void create_update_delete_should_write_audit_logs() {
        Pharmacy p = new Pharmacy();
        p.setNom("Test");
        p.setAdresse("Addr");
        p.setTelephone("000");
        p.setIce("ICE-TEST-001");
        p = pharmacyRepository.save(p);

        p.setNom("Test2");
        p = pharmacyRepository.save(p);

        pharmacyRepository.delete(p);

        assertThat(auditLogRepository.count()).isGreaterThanOrEqualTo(3);
    }
}


