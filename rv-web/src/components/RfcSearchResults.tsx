import {Component} from "react";
import Container from 'react-bootstrap/Container';
import Result from "../types/Result";
import DOMPurify from "dompurify";

interface RfcSearchResultsProps {
    results: Array<Result>;
}

interface RfcSearchResultsState {

}

class RfcSearchResults extends Component<RfcSearchResultsProps, RfcSearchResultsState> {
    constructor(props: RfcSearchResultsProps) {
        super(props);
        this.state = {}
    }

    render() {
        return (
            <Container>
                <ol>
                    {
                        this.props.results.map(function(result, index) {
                            const idLower = result.id.toLowerCase();
                            const link = `https://www.rfc-editor.org/rfc/${idLower}.html`
                            return <li key={index}>
                                <a href={link}><strong>{result.id}</strong></a>: {result.title}
                                <ul>
                                    {
                                        result.highlights.map(function(highlight, index) {
                                            const cleanHighlight = DOMPurify.sanitize(highlight, {
                                                USE_PROFILES: { html: true },
                                            });
                                            return <li key={index}>
                                                <div dangerouslySetInnerHTML={{ __html: cleanHighlight }} />
                                            </li>
                                        })
                                    }
                                </ul>
                            </li>
                        })
                    }
                </ol>
            </Container>
        )
    }
}

export default RfcSearchResults;