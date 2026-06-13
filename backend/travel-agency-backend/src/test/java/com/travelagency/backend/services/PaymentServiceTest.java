package com.travelagency.backend.services;

import com.travelagency.backend.entities.BookingEntity;
import com.travelagency.backend.entities.PaymentEntity;
import com.travelagency.backend.entities.TravelPackageEntity;
import com.travelagency.backend.entities.UserEntity;
import com.travelagency.backend.repositories.PaymentRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private PaymentService paymentService;

    private BookingEntity booking;
    private PaymentService.PaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("test@test.com");

        TravelPackageEntity pkg = new TravelPackageEntity();
        pkg.setId(1L);
        pkg.setBasePrice(new BigDecimal("500000"));

        booking = new BookingEntity();
        booking.setId(1L);
        booking.setUser(user);
        booking.setTravelPackage(pkg);
        booking.setFinalAmount(new BigDecimal("500000"));
        booking.setBookingStatus("PENDING");
        booking.setPaymentExpirationDate(LocalDateTime.now().plusHours(24));

        validRequest = new PaymentService.PaymentRequest();
        validRequest.amount = new BigDecimal("500000");
        validRequest.cardNumber = "4111111111111111";
        validRequest.cardExpirationDate = "12/28";
        validRequest.cvv = "123";
        validRequest.invoiceType = "RECEIPT";
    }

    @Test
    @DisplayName("getAllPayments() should return all payments")
        // Lista todos los pagos
    void getAllPayments_ShouldReturnAll() {
        when(paymentRepository.findAll()).thenReturn(List.of(new PaymentEntity()));

        List<PaymentEntity> result = paymentService.getAllPayments();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getPaymentById() existing should return payment")
        // Busca un pago por ID exitosamente
    void getPaymentById_WhenExists_ShouldReturn() {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentEntity result = paymentService.getPaymentById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getPaymentById() inexistent should throw")
        // Lanza excepcion si el pago no existe
    void getPaymentById_WhenNotExists_ShouldThrow() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("processPayment() happy path should succeed")
        // Caso feliz: pago exitoso, confirma reserva y genera factura
    void processPayment_HappyPath_ShouldSucceed() {
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> {
            PaymentEntity p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentEntity result = paymentService.processPayment(1L, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo("APPROVED");
        verify(bookingService).confirmBookingAfterPayment(1L);
        verify(invoiceService).generateInvoiceForPayment(any(PaymentEntity.class), eq("RECEIPT"));
    }

    @Test
    @DisplayName("processPayment() should fail when booking is CANCELED")
        // No se puede pagar una reserva cancelada
    void processPayment_WhenBookingCanceled_ShouldThrow() {
        booking.setBookingStatus("CANCELED");
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("canceled");
    }

    @Test
    @DisplayName("processPayment() should fail when booking is EXPIRED")
        // No se puede pagar una reserva expirada
    void processPayment_WhenBookingExpired_ShouldThrow() {
        booking.setBookingStatus("EXPIRED");
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("processPayment() should fail when booking already CONFIRMED")
        // No se puede pagar una reserva ya pagada
    void processPayment_WhenAlreadyConfirmed_ShouldThrow() {
        booking.setBookingStatus("CONFIRMED");
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    @DisplayName("processPayment() should fail when payment deadline expired")
        // No se puede pagar despues del plazo de 24h
    void processPayment_WhenDeadlinePassed_ShouldThrow() {
        booking.setPaymentExpirationDate(LocalDateTime.now().minusHours(1));
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    @DisplayName("processPayment() should fail when payment already exists")
        // No se puede pagar dos veces la misma reserva
    void processPayment_WhenAlreadyPaid_ShouldThrow() {
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(new PaymentEntity()));

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already has a payment");
    }

    @Test
    @DisplayName("processPayment() should fail when amount does not match")
        // El monto debe coincidir EXACTAMENTE con el monto final
    void processPayment_WhenAmountMismatch_ShouldThrow() {
        validRequest.amount = new BigDecimal("100000"); // distinto al final
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("processPayment() should fail with invalid card number")
        // Tarjeta debe tener al menos 13 digitos
    void processPayment_WithInvalidCardNumber_ShouldThrow() {
        validRequest.cardNumber = "1234";
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid card number");
    }

    @Test
    @DisplayName("processPayment() should fail with invalid expiration format")
        // Fecha de vencimiento debe ser MM/YY
    void processPayment_WithInvalidExpirationFormat_ShouldThrow() {
        validRequest.cardExpirationDate = "1228";
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid card expiration");
    }

    @Test
    @DisplayName("processPayment() should fail with invalid CVV")
        // CVV debe ser de 3 o 4 digitos
    void processPayment_WithInvalidCvv_ShouldThrow() {
        validRequest.cvv = "12";
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid CVV");
    }

    @Test
    @DisplayName("processPayment() should fail with zero or negative amount")
        // Monto debe ser positivo
    void processPayment_WithZeroAmount_ShouldThrow() {
        validRequest.amount = BigDecimal.ZERO;
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("processPayment() should mask card number correctly")
        // El numero de tarjeta debe quedar enmascarado
    void processPayment_ShouldMaskCardNumber() {
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentEntity result = paymentService.processPayment(1L, validRequest);

        assertThat(result.getMaskedCardNumber()).isEqualTo("**** **** **** 1111");
    }

    @Test
    @DisplayName("processPayment() should fail with booking in invalid state")
        // Estado inesperado del booking (no PENDING/CONFIRMED/CANCELED/EXPIRED)
    void processPayment_InvalidBookingState_ShouldThrow() {
        booking.setBookingStatus("UNKNOWN_STATE");
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalid state");
    }

    @Test
    @DisplayName("processPayment() with invoiceType INVOICE should generate invoice type INVOICE")
        // El tipo de comprobante INVOICE se pasa al InvoiceService
    void processPayment_WithInvoiceTypeInvoice_ShouldGenerateInvoice() {
        validRequest.invoiceType = "INVOICE";
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(1L, validRequest);

        verify(invoiceService).generateInvoiceForPayment(any(PaymentEntity.class), eq("INVOICE"));
    }

    @Test
    @DisplayName("processPayment() with unknown invoiceType should default to RECEIPT")
        // Tipo desconocido se cae a RECEIPT por defecto
    void processPayment_WithUnknownInvoiceType_ShouldDefaultToReceipt() {
        validRequest.invoiceType = "WEIRD_TYPE";
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(1L, validRequest);

        verify(invoiceService).generateInvoiceForPayment(any(PaymentEntity.class), eq("RECEIPT"));
    }

    @Test
    @DisplayName("processPayment() with null invoiceType should default to RECEIPT")
        // Si no se especifica tipo, default = RECEIPT
    void processPayment_WithNullInvoiceType_ShouldDefaultToReceipt() {
        validRequest.invoiceType = null;
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(1L, validRequest);

        verify(invoiceService).generateInvoiceForPayment(any(PaymentEntity.class), eq("RECEIPT"));
    }

    @Test
    @DisplayName("processPayment() should fail with null amount")
        // Monto null tambien es invalido
    void processPayment_WithNullAmount_ShouldThrow() {
        validRequest.amount = null;
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(1L, validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("getPaymentByBookingId() should return Optional")
        // Verifica el metodo que busca pago por reserva
    void getPaymentByBookingId_ShouldReturn() {
        PaymentEntity payment = new PaymentEntity();
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        Optional<PaymentEntity> result = paymentService.getPaymentByBookingId(1L);

        assertThat(result).isPresent();
    }
}