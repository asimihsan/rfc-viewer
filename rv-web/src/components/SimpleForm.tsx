import React, {Component} from "react";
import Container from 'react-bootstrap/Container';
import {Form} from "react-bootstrap";

interface SimpleFormProps {
    submitCallback: CallableFunction
}

interface SimpleFormState {
    query: String
}

class SimpleForm extends Component<SimpleFormProps, SimpleFormState> {

    constructor(props: SimpleFormProps) {
        super(props);
        this.state = {
            query: ''
        }
    }

    submit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        this.props.submitCallback(this.state.query);
    }

    render() {
        return (
            <Container>
                <Form onSubmit={this.submit}>
                    <Form.Group controlId="formName">
                        <Form.Label>RFC Viewer search</Form.Label>
                        <Form.Control
                            type="text"
                            placeholder="Enter query"
                            onChange={e => this.setState({query: e.target.value})}
                        />
                    </Form.Group>
                </Form>
            </Container>
        );
    }
}

export default SimpleForm;