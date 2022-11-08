import React, {Component} from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import {Container} from "react-bootstrap";
import SimpleForm from "./components/SimpleForm";
import './App.css';
import axios from "axios";
import RfcSearchResults from "./components/RfcSearchResults";
import Result from "./types/Result";

const apiUrl = "https://spec.ninja/api/2022-11-07";

interface AppProps {

}

interface AppState {
    results: Array<Result>,
}

class App extends Component<AppProps, AppState> {
    constructor(props: AppProps) {
        super(props);
        this.state = {
            results: [],
        }
    }

    callback = (query: String) => {
        console.log("app callback!");
        console.log(query);
        axios.post(apiUrl, {
            'query': query
        }).then((response) => {
            console.log(response);
            this.setState({
                results: response.data,
            })
        }).catch((error) => {
            console.log(error);
        });
    }

    render() {
        return (
            <Container>
                <div>
                    <SimpleForm
                        submitCallback={this.callback}
                    />
                </div>
                <div>
                    <RfcSearchResults
                        results={this.state.results}
                    />
                </div>
            </Container>
        );
    }
}

export default App;
