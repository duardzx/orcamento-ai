package com.dio.orcamentoai.service;

import com.dio.orcamentoai.model.Transacao;
import com.dio.orcamentoai.repository.TransacaoRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrcamentoService {

    private final TransacaoRepository transacaoRepository;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ChatClient chatClient;

    public OrcamentoService(TransacaoRepository transacaoRepository,
                            OpenAiAudioTranscriptionModel transcriptionModel,
                            ChatClient.Builder chatClientBuilder) {
        this.transacaoRepository = transacaoRepository;
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClientBuilder.build();
    }

    public Transacao processarAudioEGravarTransacao(MultipartFile arquivoAudio) {
        try {
            // 1. Transcrever áudio usando Whisper (Spring AI)
            ByteArrayResource resource = new ByteArrayResource(arquivoAudio.getBytes()) {
                @Override
                public String getFilename() {
                    return arquivoAudio.getOriginalFilename();
                }
            };

            String textoTranscrito = transcriptionModel.call(new AudioTranscriptionPrompt(resource))
                    .getResult()
                    .getOutput();

            // 2. Processar a intenção do texto usando a IA (ChatClient)
            String promptSistema = """
                    Analise o texto a seguir e extraia os dados para uma transação financeira.
                    Texto: "%s"
                    
                    Responda estritamente no formato JSON abaixo e nada mais:
                    {
                      "descricao": "breve descrição",
                      "valor": 0.00,
                      "tipo": "ENTRADA ou SAIDA",
                      "categoria": "ex: Alimentação, Transporte, Lazer, Salário, etc."
                    }
                    """.formatted(textoTranscrito);

            String respostaJson = chatClient.prompt()
                    .user(promptSistema)
                    .call()
                    .content();

            // 3. Determinar o tipo da transação
            Transacao.TipoTransacao tipoCalculado = (textoTranscrito.toLowerCase().contains("ganhei") 
                    || textoTranscrito.toLowerCase().contains("recebi")) 
                    ? Transacao.TipoTransacao.ENTRADA 
                    : Transacao.TipoTransacao.SAIDA;

            // 4. Salvar usando o construtor tradicional em vez do builder
            Transacao novaTransacao = new Transacao(
                    "Transação via Voz: " + textoTranscrito,
                    new BigDecimal("50.00"),
                    tipoCalculado,
                    "Geral"
            );

            return transacaoRepository.save(novaTransacao);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar áudio com IA: " + e.getMessage(), e);
        }
    }

    public List<Transacao> listarTodas() {
        return transacaoRepository.findAll();
    }
}