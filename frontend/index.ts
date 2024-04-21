import { createElement } from 'react';
import App from './App.js';
import {createRoot} from "react-dom/client";

createRoot(document.getElementById('outlet')!).render(createElement(App));
