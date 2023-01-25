package com.example.demobigfilesreactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.READ;

@CrossOrigin(origins = "*", methods = {RequestMethod.GET})
@RestController
@RequestMapping(path = "/read")
public class AnimesController {
    private final String PATH = "./animeflv.csv";
    private final AtomicInteger count = new AtomicInteger(1);
    private static final Logger log = LoggerFactory.getLogger(AnimesController.class);

    @GetMapping(produces = {MediaType.APPLICATION_NDJSON_VALUE})
    public Flux<AnimeModel> read() {
        return DataBufferUtils.readAsynchronousFileChannel(() -> AsynchronousFileChannel.open(Path.of(PATH), READ),
                        DefaultDataBufferFactory.sharedInstance, 4096
                )
                .transform(dataBufferFlux -> StringDecoder.textPlainOnly()
                        .decode(dataBufferFlux, ResolvableType.NONE, null, null))
                .skip(1)
                .index()
                .delayElements(Duration.ofMillis(100L))
                .map(s -> {
                    var line = s.getT2();
                    String[] items = line.split(",(?=(?:[^\"\\n]*\"[^\"\\n]*\")*[^\"\\n]*$)");
                    count.getAndIncrement();
                    var desc = items[1].replace("\"", "");
                    var description = desc.substring(0, Math.min(desc.length(), 100));
                    return new AnimeModel(s.getT1(), items[0], description, items[10]);
                })
                .publishOn(Schedulers.boundedElastic())
                .doFinally(s -> {
                    log.info("msg=Cancelando ou requisição terminada; counter={}", count.decrementAndGet());
                    count.set(1);
                });
    }
}
