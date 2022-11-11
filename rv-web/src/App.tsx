import React, {Component} from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import {Container} from "react-bootstrap";
import SimpleForm from "./components/SimpleForm";
import './App.css';
import axios from "axios";
import RfcSearchResults from "./components/RfcSearchResults";
import Result from "./types/Result";
import ReactLoading from "react-loading";

const apiUrl = "https://spec.ninja/api/2022-11-07";

interface AppProps {

}

interface AppState {
    results: Array<Result>,
    isLoading: boolean,
}

class App extends Component<AppProps, AppState> {
    constructor(props: AppProps) {
        super(props);
        this.state = {
            results: [],
            isLoading: false,
        }
    }

    callback = (query: String) => {
        console.log("app callback!");
        console.log(query);
        this.setState({isLoading: true}, () => {
            axios.post(apiUrl, {
                'query': query
            }).then((response) => {
                console.log(response);
                this.setState({
                    results: response.data,
                })
            }).catch((error) => {
                console.log(error);
            }).finally(() => {
                this.setState({
                    isLoading: false,
                });
            });
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
                {this.state.isLoading && (
                    <ReactLoading
                        type={"spokes"}
                        color={"#ccc"}
                        height={"4%"}
                        width={"4%"}
                    />
                )}
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
