package com.frontpet.notifications;

/**
 * Contenido del email de reset de contraseña (PT-BR, ADR 007). Sin librería
 * de templates (Thymeleaf, etc.): {@code String.formatted} alcanza para un
 * único email con un único valor interpolado — el link, generado por
 * nosotros, no por el usuario (no hay que pensar en XSS acá, por eso el
 * saludo es "Olá," a secas, sin nombre).
 *
 * <p>HTML mínimo, con estilos inline (los clientes de mail ignoran
 * {@code <style>}) y sin imágenes externas (disparan filtros de spam y
 * avisan al remitente que el mail se abrió).
 */
public final class PasswordResetEmailTemplate {

    private PasswordResetEmailTemplate() {
    }

    public static String subject() {
        return "Redefinição de senha — Painel FrontPet";
    }

    public static String text(String link) {
        return """
                Olá,

                Recebemos um pedido para redefinir a senha do seu acesso ao painel administrativo da FrontPet.

                Para criar uma nova senha, acesse o link abaixo:
                %s

                Este link vale por 60 minutos e só pode ser usado uma vez. Se você pedir outro link, este deixa de funcionar.

                Se não foi você quem fez este pedido, ignore esta mensagem — sua senha continua a mesma.

                Esta é uma mensagem automática, não responda este e-mail.
                """.formatted(link);
    }

    public static String html(String link) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <body style="margin:0;padding:0;background-color:#F1F5F9;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F1F5F9;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#FFFFFF;border-radius:16px;padding:32px;">
                          <tr>
                            <td style="font-size:15px;line-height:1.5;color:#0F172A;">
                              <p>Olá,</p>
                              <p>Recebemos um pedido para redefinir a senha do seu acesso ao painel administrativo da FrontPet.</p>
                              <p style="text-align:center;margin:32px 0;">
                                <a href="%s" style="background-color:#011E5A;color:#FFFFFF;text-decoration:none;padding:12px 24px;border-radius:8px;display:inline-block;font-weight:600;">Redefinir senha</a>
                              </p>
                              <p style="font-size:13px;color:#64748B;">Ou copie e cole este link no navegador:<br>%s</p>
                              <p>Este link vale por 60 minutos e só pode ser usado uma vez. Se você pedir outro link, este deixa de funcionar.</p>
                              <p>Se não foi você quem fez este pedido, ignore esta mensagem — sua senha continua a mesma.</p>
                              <p style="font-size:12px;color:#94A3B8;margin-top:32px;">Esta é uma mensagem automática, não responda este e-mail.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(link, link);
    }
}
