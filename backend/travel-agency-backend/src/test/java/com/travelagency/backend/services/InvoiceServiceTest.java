package com.travelagency.backend.services;

import com.travelagency.backend.entities.*;
import com.travelagency.backend.repositories.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private PaymentEntity payment;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setFullName("Test User");
        user.setEmail("test@test.com");
        user.setIdDocument("12345678-9");

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setName("Test Pkg");
        pkg.setDestination("Chile");
        pkg.setStartDate(java.time.LocalDate.now().plusDays(10));
        pkg.setEndDate(java.time.LocalDate.now().plusDays(15));

        BookingEntity booking = new BookingEntity();
        booking.setUser(user);
        booking.setTravelPackage(pkg);
        booking.setBookingCode("BK-001");
        booking.setPassengerCount(2);
        booking.setGrossAmount(new BigDecimal("1000000"));
        booking.setDiscountAmount(BigDecimal.ZERO);

        payment = new PaymentEntity();
        payment.setId(1L);
        payment.setBooking(booking);
        payment.setAmountPaid(new BigDecimal("1000000"));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setMaskedCardNumber("**** **** **** 1111");
        payment.setInternalTransactionId("TXN-2026-AAA");
    }

    @Test
    @DisplayName("getAllInvoices() should return all invoices")
        // Lista todas las facturas
    void getAllInvoices_ShouldReturnAll() {
        when(invoiceRepository.findAll()).thenReturn(List.of(new InvoiceEntity()));

        List<InvoiceEntity> result = invoiceService.getAllInvoices();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getInvoiceById() existing should return invoice")
        // Busca por ID exitosamente
    void getInvoiceById_WhenExists_ShouldReturn() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(1L);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InvoiceEntity result = invoiceService.getInvoiceById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getInvoiceById() inexistent should throw")
        // Lanza excepcion si no existe
    void getInvoiceById_WhenNotExists_ShouldThrow() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getInvoiceByPaymentId() should return invoice")
        // Busca factura por ID de pago
    void getInvoiceByPaymentId_ShouldReturn() {
        when(invoiceRepository.findByPaymentId(1L)).thenReturn(Optional.of(new InvoiceEntity()));

        Optional<InvoiceEntity> result = invoiceService.getInvoiceByPaymentId(1L);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("getInvoiceByFolio() should return invoice")
        // Busca factura por numero de folio
    void getInvoiceByFolio_ShouldReturn() {
        when(invoiceRepository.findByFolioNumber("B-2026-00001")).thenReturn(Optional.of(new InvoiceEntity()));

        Optional<InvoiceEntity> result = invoiceService.getInvoiceByFolio("B-2026-00001");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("generateInvoiceForPayment() RECEIPT should create with B-prefix folio")
        // Genera boleta con prefijo B
    void generateInvoiceForPayment_Receipt_ShouldCreateWithBPrefix() {
        when(invoiceRepository.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.countByInvoiceType("RECEIPT")).thenReturn(0L);
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = invoiceService.generateInvoiceForPayment(payment, "RECEIPT");

        assertThat(result.getInvoiceType()).isEqualTo("RECEIPT");
        assertThat(result.getFolioNumber()).startsWith("B-");
    }

    @Test
    @DisplayName("generateInvoiceForPayment() INVOICE should create with F-prefix folio")
        // Genera factura con prefijo F
    void generateInvoiceForPayment_Invoice_ShouldCreateWithFPrefix() {
        when(invoiceRepository.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.countByInvoiceType("INVOICE")).thenReturn(0L);
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = invoiceService.generateInvoiceForPayment(payment, "INVOICE");

        assertThat(result.getInvoiceType()).isEqualTo("INVOICE");
        assertThat(result.getFolioNumber()).startsWith("F-");
    }

    @Test
    @DisplayName("generateInvoiceForPayment() should throw if invoice already exists")
        // No se puede generar dos facturas para el mismo pago
    void generateInvoiceForPayment_WhenAlreadyExists_ShouldThrow() {
        when(invoiceRepository.findByPaymentId(1L)).thenReturn(Optional.of(new InvoiceEntity()));

        assertThatThrownBy(() -> invoiceService.generateInvoiceForPayment(payment, "RECEIPT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("generateInvoiceForPayment() should set snapshot details")
        // El snapshot debe incluir datos del cliente, reserva y pago
    void generateInvoiceForPayment_ShouldSetSnapshot() {
        when(invoiceRepository.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.countByInvoiceType(anyString())).thenReturn(0L);
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceEntity result = invoiceService.generateInvoiceForPayment(payment, "RECEIPT");

        assertThat(result.getSnapshotDetails()).contains("Test User");
        assertThat(result.getSnapshotDetails()).contains("BK-001");
        assertThat(result.getSnapshotDetails()).contains("TXN-2026-AAA");
    }
}