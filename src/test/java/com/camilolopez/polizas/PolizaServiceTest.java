package com.camilolopez.polizas;

import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.exception.BusinessRuleException;
import com.camilolopez.polizas.integration.CoreClient;
import com.camilolopez.polizas.repository.*;
import com.camilolopez.polizas.service.PolizaService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolizaServiceTest {
    @Test void successiveRenewalsPreserveMonthsAndApplyIpcAgain() {
        PolizaRepository polizas = mock(PolizaRepository.class);
        RiesgoRepository riesgos = mock(RiesgoRepository.class);
        CoreClient core = mock(CoreClient.class);
        Poliza p = new Poliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA,
                LocalDate.of(2026, 1, 31), 1, new BigDecimal("1000.00"));
        p.id = 1L;
        when(polizas.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        PolizaService service = new PolizaService(polizas, riesgos, core);
        var first = service.renovar(1L, new BigDecimal("5"));
        assertEquals(new BigDecimal("1050.00"), first.canonMensual());
        assertEquals(LocalDate.of(2026, 2, 28), first.fechaInicio());
        assertEquals(LocalDate.of(2026, 3, 28), first.fechaFin());
        var second = service.renovar(1L, BigDecimal.ZERO);
        assertEquals(new BigDecimal("1050.00"), second.canonMensual());
        assertEquals(LocalDate.of(2026, 3, 28), second.fechaInicio());
        assertEquals(1, second.mesesVigenciaInicial());
        verify(core, times(2)).actualizacion(1L, "RENOVAR");
    }
    @Test void cancelledPolicyDoesNotCallCore() {
        PolizaRepository polizas = mock(PolizaRepository.class);
        CoreClient core = mock(CoreClient.class);
        Poliza p = new Poliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.CANCELADA,
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000.00"));
        p.id = 3L;
        when(polizas.findByIdForUpdate(3L)).thenReturn(Optional.of(p));
        assertThrows(BusinessRuleException.class,
                () -> new PolizaService(polizas, mock(RiesgoRepository.class), core).renovar(3L, new BigDecimal("5")));
        verifyNoInteractions(core);
    }
}
