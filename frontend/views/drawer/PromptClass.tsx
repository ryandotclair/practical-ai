import { useForm } from '@hilla/react-form';

import {AzureChatController, PersistMessages} from 'Frontend/generated/endpoints';
import {TextField} from "@hilla/react-components/TextField";
import {Button} from "@hilla/react-components/Button";
import {StringModel} from "@hilla/form";
import {useEffect, useState} from "react";
import {TextArea} from "@hilla/react-components/TextArea";
import * as React from "react";

export default function PromptClass({...props}) {
    const [ip, setIp] = useState()
    const prompt = useForm(StringModel);
    const [working, setWorking] = useState(true);
    const [previousValue, setValue] = useState("")
    const { model, read, submit, field, submitting } = useForm(StringModel, {
        onSubmit: async (prompt) => {
            if (!(previousValue == prompt)) {
                await AzureChatController.savePrompt(prompt, ip);
                props.change[1]();
                setValue(prompt)
                setWorking(true)
            }
        }
    });
    const getIp = async () => {
        // Connect ipapi.co with fetch()
        const response = await fetch("https://ipapi.co/json/")
        const data = await response.json()
        // Set the IP address to the constant `ip`
        setIp(data.ip)
        PersistMessages.getPrompts(data.ip).then((value) => {
            setValue(value? value: "");
            read(value);
        })
    }

    const cancel = async () => {
        // Connect ipapi.co with fetch()
        props.change[0]();
    }


    // Run `getIP` function above just once when the page is rendered
    useEffect(() => {
        getIp()
    }, [])


    return (
        <section style={{"marginLeft": "5%"}}>
            <p style={{"textAlign": "center", "fontFamily": "SÃ¶hne,helvetica,sans-serif", "width": "400px", "borderRadius": "10px", backgroundColor: "white", "fontWeight": "bold"}}>System Prompt

            <TextArea
                 style={{ "backgroundColor": "white", "fontFamily": "SÃ¶hne,helvetica,sans-serif", "borderRadius": "20px", width: '100%', minHeight: '100%', maxHeight: '100%' }}
                {...field(model)}
                onFocus={() => setWorking(false)}
                onChange={() => setWorking(false)}
            />
            <Button
                style={{"backgroundColor": working? "":"#10a37f", "fontFamily": "SÃ¶hne,helvetica,sans-serif", "float": "right", "color": working? "":"white", "borderRadius": "10px"}}
                onClick={submit}
                disabled={working}>Save</Button>
            <Button
               style={{"backgroundColor": "#ececf1", "fontFamily": "SÃ¶hne,helvetica,sans-serif", "float": "right", "color": "black", "borderRadius": "10px"}}
                onClick={cancel}
                >Cancel</Button>
            </p>
        </section>
    );

}