import * as React from 'react';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import Button from '@mui/material/Button';
import DrawerData from "Frontend/views/drawer/DrawerData";
import ChatHistory from "Frontend/views/chat/ChatHistory";
import {PersistMessages} from "Frontend/generated/endpoints";
import {useState} from "react";
import { createTheme } from '@mui/material/styles';
import PromptClass from "Frontend/views/drawer/PromptClass";
import {IconButton, Tooltip} from "@mui/material";


type Anchor = 'top' | 'left' | 'bottom' | 'right';
export default function TemporaryDrawer({...props}) {
    const [openSideCar, setSideCar] = React.useState(false);
    const [openResponse, setResponse] = React.useState(false);
    const [openPrompt, setPrompt] = React.useState(false);
    const [ip, setIp] = useState()

    const [state, setState] = React.useState({
        top: false,
        left: false,
        bottom: false,
        right: false,
    });

    const toggleDrawer =
        (anchor: Anchor, open: boolean) =>
            (event: React.KeyboardEvent | React.MouseEvent) => {
                if (
                    event.type === 'keydown' &&
                    ((event as React.KeyboardEvent).key === 'Tab' ||
                        (event as React.KeyboardEvent).key === 'Shift')
                ) {
                    return;
                }

                setState({...state, [anchor]: open});
            };

    const getIp = async () => {
        // Connect ipapi.co with fetch()
        const response = await fetch("https://ipapi.co/json/")
        const data = await response.json()
        setIp(data.ip);
        PersistMessages.reset(data.ip);
        window.location.reload();
    }

    // Run `getIP` function above just once when the page is rendered

  const list = (openSideCar: boolean, props: []) => (
    <Box
        px={3} py={1}
        role="presentation"
        onKeyDown={() => setSideCar(false)}
    >
        <DrawerData/>
        <Button
            style={{textTransform: "none", "backgroundColor": "#ececf1", "fontFamily": "SÃ¶hne,helvetica,sans-serif", "float": "right", "color": "black", "borderRadius": "10px"}}
            onClick={() => setSideCar(false)}
        >Close</Button>
    </Box>
  );
    const list2 = () => (
        <Box px={3} py={1}
            role="presentation"
            onKeyDown={() => setResponse(false)}
        >
       <ChatHistory/>
            <Button
                style={{textTransform: "none", "backgroundColor": "#ececf1", "fontFamily": "SÃ¶hne,helvetica,sans-serif", "float": "right", "color": "black", "borderRadius": "10px"}}
                onClick={() => setResponse(false)}
            >Close</Button>
        </Box>
    );

    const list3 = () => (
        <Box px={3} py={1}
             role="presentation"
             // onClick={() => setPrompt(false)}
        >
            <PromptClass change = {[handleState, props.cleanup]}/>
        </Box>
    );
    function handleState() {
        setPrompt(false);
    }
  return (
    <div>
        <React.Fragment>
            <p style={{float:"left", "textAlign": "center", "fontFamily": "SÃ¶hne,helvetica,sans-serif", "marginLeft": "30%", "width": "300px", "borderRadius": "10px", backgroundColor: "white", "fontWeight": "bold"}}> Vee - Your Movie Assistant
            </p>
            <Tooltip title="New Chat">
            <IconButton style={{
                    overflow: "hidden",
                    borderRadius: 10,
                    backgroundColor: "white",
                    padding: "1px 1px",
                    fontSize: "16px",
                    textTransform: "none",
                    fontWeight: "bold",
                    margin: "4px"
            }}
                        className={"button-drawer"} onClick={() => getIp()}>
                    💬
                </IconButton>
            </Tooltip>
            <Tooltip title="Behind the Scene">
            <IconButton style={{
                overflow: "hidden",
                borderRadius: 10,
                backgroundColor: "#ececf1",
                padding: "2px 2px",
                fontSize: "14px",
                textTransform: "none",
                fontWeight: "bold",
                margin: "4px",
            }}  className={"button-drawer"} onClick={() => setSideCar(true)}>
                <div style={{"color": "black"}}>🚪</div>
            </IconButton>
            </Tooltip>
            <Tooltip title="ChatCompletionMessage Content">
            <IconButton style={{
                overflow: "hidden",
                borderRadius: 10,
                backgroundColor: "#ececf1",
                textTransform: "none",
                padding: "2px 2px",
                fontSize: "14px",
                fontWeight: "bold",
                margin: "4px",
            }} className={"button-drawer"} onClick={() => setResponse(true)}>
                🎙️
            </IconButton>
            </Tooltip>
            <Tooltip title="Change Prompt">
                <IconButton style={{
                overflow: "hidden",
                borderRadius: 10,
                backgroundColor: "#ececf1",
                color: "white",
                padding: "2px 2px",
                fontSize: "14px",
                textTransform: "none",
                fontWeight: "bold",
                margin: "4px",
            }} className={"button-drawer"} onClick={() => setPrompt(true)}>
                <div style={{"color": "black"}}>📝</div>
            </IconButton>
            </Tooltip>
          <Drawer
              PaperProps={{
                  sx: {
                      width: {
                          xs: 350, // theme.breakpoints.up('xs')
                          sm: 450, // theme.breakpoints.up('sm')
                          md: 450, // theme.breakpoints.up('md')
                          lg: 450, // theme.breakpoints.up('lg')
                          xl: 500, // theme.breakpoints.up('xl')

                      },
                      margin: 14,
                      height: {
                          xs: 550, // theme.breakpoints.up('xs')
                          sm: 550, // theme.breakpoints.up('sm')
                          md: 550, // theme.breakpoints.up('md')
                          lg: 600, // theme.breakpoints.up('lg')
                          xl: 800, // theme.breakpoints.up('xl')

                      },
                      "borderRadius": "10px"
                 }}}
            anchor='right'
            open={openSideCar}
            onClose={() => setSideCar(false)}
          >
            {list(openSideCar, props.props)}
          </Drawer>
            <Drawer
                PaperProps={{
                    sx: {
                        width: {
                            xs: 330, // theme.breakpoints.up('xs')
                            sm: 350, // theme.breakpoints.up('sm')
                            md: 450, // theme.breakpoints.up('md')
                            lg: 450, // theme.breakpoints.up('lg')
                            xl: 500, // theme.breakpoints.up('xl')

                        },
                        margin: 14,
                        height: {
                            xs: 550, // theme.breakpoints.up('xs')
                            sm: 550, // theme.breakpoints.up('sm')
                            md: 550, // theme.breakpoints.up('md')
                            lg: 600, // theme.breakpoints.up('lg')
                            xl: 800, // theme.breakpoints.up('xl')

                        },
                        "borderRadius": "10px"
                    }}}
                anchor='right'
                open={openResponse}
                onClose={() => setResponse(false)}
            >
                {list2()}
            </Drawer>
            <Drawer
                PaperProps={{
                    sx: {
                        width: {
                            xs: 330, // theme.breakpoints.up('xs')
                            sm: 350, // theme.breakpoints.up('sm')
                            md: 450, // theme.breakpoints.up('md')
                            lg: 450, // theme.breakpoints.up('lg')
                            xl: 500, // theme.breakpoints.up('xl')

                        },
                        margin: 14,
                        height: {
                            xs: 550, // theme.breakpoints.up('xs')
                            sm: 550, // theme.breakpoints.up('sm')
                            md: 550, // theme.breakpoints.up('md')
                            lg: 600, // theme.breakpoints.up('lg')
                            xl: 800, // theme.breakpoints.up('xl')

                        },
                        "borderRadius": "10px"
                    }}}
                anchor='right'
                open={openPrompt}
                onClose={() => setPrompt(false)}
            >
                {list3()}
            </Drawer>
        </React.Fragment>
    </div>
  );
}