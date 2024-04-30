import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import React, {useEffect, useState} from "react";
import {PersistMessages} from "Frontend/generated/endpoints";
import {VirtualList} from "@hilla/react-components/VirtualList";
import ChatMessage from "Frontend/views/chat/ChatMessage";

export default function BehindTheCurtain() {
    const [messages, setMessages] = useState<any>();
    const [ip, setIp] = useState()
    // Prepare a constant `ip` with empty data by default

    const getIp = async () => {
        // Connect ipapi.co with fetch()
        const response = await fetch("https://ipapi.co/json/")
        const data = await response.json()
        // Set the IP address to the constant `ip`
        setIp(data.ip)
        let newResponse = [];
        const messageResponse = await PersistMessages.getLogs(data.ip);
        if (messageResponse != undefined) {
            for (var i = 0; i < messageResponse?.length; i++) {
                newResponse[i] = messageResponse[i];
            }
            setMessages(newResponse.join("\n\n"));
        }
        console.log(newResponse.join(" "));
    }

    // Run `getIP` function above just once when the page is rendered
    useEffect(() => {
        getIp()
    }, [])


    return (
        <>
            <div>
                <h2 className="text-l text-center m-0">
                    <p>Behind the Scene</p>
                </h2>
                 <ReactMarkdown children={messages? messages: ""} rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]} />
               </div>
        </>
    );
}