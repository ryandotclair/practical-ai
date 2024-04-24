import * as React from "react";
import {useEffect, useState} from "react";
import ChatMessage from "Frontend/views/chat/ChatMessage";
import "@chatscope/chat-ui-kit-styles/dist/default/styles.min.css";
import moment from 'moment';
import TemporaryDrawer from "Frontend/views/drawer/Drawers";
import {AzureChatController} from "Frontend/generated/endpoints";
import Role from "Frontend/generated/com/azure/spring/movee/model/ChatCompletionMessage/Role";
import ChatCompletionMessage from "Frontend/generated/com/azure/spring/movee/model/ChatCompletionMessage";
// import {
//     Avatar,
//     Message,
//     MessageInput,
//     MessageList,
//     MessageSeparator,
//     TypingIndicator,
// } from "@chatscope/chat-ui-kit-react";
import {VirtualList} from "@hilla/react-components/VirtualList";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import {Message, MessageList, MessageInput, MessageSeparator, TypingIndicator} from "@chatscope/chat-ui-kit-react";

export default function ChatView({...props}) {
    const [ip, setIp] = useState()
    const [date, setDate] = useState("")
    const [working, setWorking] = useState(false);
    const [messages, setMessages] = useState<ChatCompletionMessage[]>([]);
    const [isChatbotTyping, setIsChatbotTyping] = useState(false);
    const [isChatbotThinking, setIsChatbotThinking] = useState(false);
    async function getCompletion(text: string) {
        if (working) return;
        setWorking(true);
        setIsChatbotThinking(true);
        console.log("text:" +" "+ text)
        const messageHistory = [
            ...messages,
            {
                role: Role.USER,
                content: text,
            },
        ];

        // Display the question
        setMessages(messageHistory);

        // Add a new message to the list on the first response chunk, then append to it
        let firstChunk = true;

        function appendToLastMessage(chunk: string) {
            setIsChatbotThinking(false);
            setIsChatbotTyping(true);
            if (firstChunk) {
                // Init the response message on the first chunk
                setMessages((msg) => [
                    ...msg,
                    {
                        role: Role.ASSISTANT,
                        content: '',
                    },
                ]);
                firstChunk = false;
            } else {

                setMessages((msg) => {
                    const lastMessage = msg[msg.length - 1];
                    lastMessage.content += chunk;
                    return [...msg.slice(0, -1), lastMessage];
                });
            }
        }

        // Get completion as stream
        AzureChatController.getChats(messageHistory, ip)
            .onNext((chunk) => appendToLastMessage(chunk?chunk:""))
            .onComplete(() => {
                setWorking(false);
                setIsChatbotTyping(false)
            })
            .onError(() => {
                console.error('Error processing stream');
                setWorking(false);
            });
    }


        // Prepare a constant `ip` with empty data by default

        const getIp = async () => {
            // Connect ipapi.co with fetch()
            const response = await fetch("https://ipapi.co/json/")
            const data = await response.json()
            // Set the IP address to the constant `ip`
            setIp(data.ip)
            let currentDate = moment().format('MMMM Do YYYY');
            setDate(currentDate)
    }


    // Run `getIP` function above just once when the page is rendered
    useEffect(() => {
        getIp()
    }, [])


    function cleanup () {
        setMessages(messages => []);
    }
    return (
        <div className={"chat-container"}>
            <TemporaryDrawer cleanup = {cleanup}></TemporaryDrawer>
            <MessageList
                typingIndicator={
                    isChatbotThinking ? <TypingIndicator style={{"backgroundColor": "#383D38", "color": "white", "fontWeight": "bold", "fontFamily" : "SÃ¶hne,helvetica,sans-serif"}} content="Vee is thinking" />
                        : isChatbotTyping ? <TypingIndicator style={{"backgroundColor": "#383D38", "color": "white", "fontWeight": "bold", "fontFamily" : "SÃ¶hne,helvetica,sans-serif"}} content="Vee is typing" />
                        : null
                } >
                {messages.map((item, i) => {
                    console.log("item: " +item.content)
                    return <Message model={{
                        direction: item?.role == "USER"?"outgoing":"incoming",
                        position: "single"
                    }}
                    >

                        <Message.CustomContent>
                            <ReactMarkdown rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]}>{item?.content || ''}</ReactMarkdown>
                        </Message.CustomContent>

                    </Message>
                })}
            </MessageList>
            <MessageInput
                placeholder="Type Message here"
                onSend={getCompletion}
                attachButton = {false}
                disabled={isChatbotThinking || isChatbotTyping}
                onPaste={(evt) => { evt.preventDefault(); document.execCommand('insertText', false, evt.clipboardData.getData("text")); }}
            />
        </div>
    );
}

