import React, {useEffect, useState} from "react";
import {MessageList, MessageListItem} from "@hilla/react-components/MessageList";
import {PersistMessages} from "Frontend/generated/endpoints";

 export default function ChatHistory() {
     const [messages, setMessages] = useState<any[]>();
     const [ip, setIp] = useState()
        // Prepare a constant `ip` with empty data by default

        const getIp = async () => {
            // Connect ipapi.co with fetch()
            const response = await fetch("https://ipapi.co/json/")
            const data = await response.json()
            // Set the IP address to the constant `ip`
            setIp(data.ip)
            let newResponse = [];
           const messageResponse = await PersistMessages.retrieveMessages(data.ip);
            if (messageResponse != undefined) {
                for (var i = 0; i < messageResponse?.length; i++) {
                     newResponse[i] = messageResponse[i];
                }
                setMessages(newResponse);
            }
        }

        // Run `getIP` function above just once when the page is rendered
        useEffect(() => {
            getIp()
        }, [])


     return (
        <>
            <div>
                <h2 className="text-l text-center m-0">
                    <p>ChatRequestMessage Content</p>
                </h2>
             <pre>

                 {JSON.stringify(messages, null, 1) }

                 </pre>
            </div>
        </>
    );
}