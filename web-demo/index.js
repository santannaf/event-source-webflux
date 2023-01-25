const API_URL = "http://localhost:3000/read"

async function consumeAPI(signal) {
    const response = await fetch(API_URL, {
        signal
    })

    return response.body
        .pipeThrough(new TextDecoderStream())
        .pipeThrough(parseNDJSON())
    /*.pipeTo(new WritableStream({
        write(chunk) {
            console.log('chunk', chunk)
        }
    }))*/
}

function appendToHTML(element) {
    return new WritableStream({
        write({title, description, url_anime}) {
            const card = `
            <article>
                <div class="text">
                    <h4>${title}</h4>
                    <p>${description}</p>
                    <a href="${url_anime}">lets go</a>
                </div>
            </article>
            `
            element.innerHTML += card
        },
        abort(_) {
            console.log('aborted loading events')
        }
    })
}

function parseNDJSON() {
    let ndjsonBuffer = ''
    return new TransformStream({
        transform(chunk, controller) {
            ndjsonBuffer += chunk
            const items = ndjsonBuffer.split('\n')
            items.slice(0, -1)
                .forEach(item => controller.enqueue(JSON.parse(item)))

            ndjsonBuffer = items[items.length - 1]
        },
        flush(controller) {
            if (!ndjsonBuffer) return
            controller.enqueue(JSON.parse(ndjsonBuffer))
        }
    })
}

const [
    start,
    stop,
    cards
] = ['start', 'stop', 'cards'].map(item => document.getElementById(item))

let aboortController = new AbortController()
start.addEventListener('click', async () => {
    cards.innerHTML = ''
    const readable = await consumeAPI(aboortController.signal)
    await readable.pipeTo(appendToHTML(cards))
})

stop.addEventListener('click', async () => {
    aboortController.abort()
    aboortController = new AbortController()
})