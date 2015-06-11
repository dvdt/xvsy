var XvsyRoot = React.createClass({displayName: "XvsyRoot",
        handlePlotClick: function (aesState) {
            $.ajax('/api/v1/plot', {
                type: "GET",
                data: {"spec": JSON.stringify(aesState),
                    "height": document.getElementById('plot-result').clientHeight,
                    "width": document.getElementById('plot-result').clientWidth,
                    "inline": true
                },
                contentType: 'application/json; charset=utf-8',
                dataType: "html",
                success: function(data, status, req) {
                    this.setState({svg: data});
                }.bind(this),
                error: function(xhr, status, err) {
                    console.error('api plot', status, err.toString());
                }.bind(this)
            });
        },
        handleSpecUpdate : function (spec) {
            this.setState({dataset: spec.dataset});
        },
        getInitialState: function () {
            return {
                svg: null,
                table: [],
                dataset: null
            }
        },
        componentDidMount: function () {
        },
        render: function () {
            return (
                React.createElement("div", {className: "row"}, 
                    React.createElement("div", {className: "col-md-3"}, 
                        React.createElement(SidePanelForm, {handlePlotClick: this.handlePlotClick, handlePlotSpec: this.handleSpecUpdate})
                    ), 
                    React.createElement("div", {className: "col-md-9"}, 
                        React.createElement("div", {className: "row"}, 
                            React.createElement("div", {className: "col-md-12"}, 
                                React.createElement(PlotResult, {data: this.state.svg})
                            )
                        ), 
                        React.createElement("div", {className: "row"}, 
                            React.createElement("div", {className: "col-md-12"}, 
                                this.state.dataset ? React.createElement(DatasetSummary, {dataset: this.state.dataset}) : null
                            )
                        )
                    )
                ));
        }}
);

var SidePanelForm = React.createClass({displayName: "SidePanelForm",
    mixins: [FormMixin, UrlInitState],
    render: function () {
        var dataset = this.state.schema.dataset ?
            React.createElement(DatasetSelector, {handleInput: this.handleInput, value: this.state.current.dataset, 
                             vals: this.state.schema.dataset}) : null;
        var whereSelectors = (React.createElement(WhereSelectors, {schema: this.state.schema.where ? this.state.schema.where : null, 
                                              where: this.state.current.where ? this.state.current.where : null, 
                                              handleInput: this.handleInput}));
        return (
            React.createElement("div", {id: "xvsy-form"}, 
                React.createElement("div", {className: "form-group"}, 
                    React.createElement("label", {className: "h4", htmlFor: "dataset"}, "Dataset"), 
                    React.createElement("div", {id: "dataset"}, 
                        dataset
                    )
                ), 

                React.createElement("hr", null), 

                React.createElement("div", {className: "form-group"}, 
                    React.createElement("label", {className: "h4", htmlFor: "geom-form"}, "Geom"), 
                    React.createElement(GeomSelector, {id: "geom-form", handleInput: this.handleInput, geom: this.state.current.geom, 
                        children: React.createElement(GeomArray, null)})
                ), 

                React.createElement("hr", null), 

                React.createElement("div", {className: "form-group"}, 
                    React.createElement("label", {className: "h4", htmlFor: "aes-form"}, "Aesthetics"), 
                    this.state.schema.aesthetics ?
                        React.createElement(Aesthetics, {id: "aes-form", handleInput: this.handleInput, aesthetics: this.state.current.aesthetics, 
                                    schema: this.state.schema.aesthetics}) : null
                ), 

                React.createElement("hr", null), 

                React.createElement("div", {className: "form-group"}, 
                    React.createElement("label", {className: "h4", htmlFor: "where-form"}, "Filters"), 
                    this.state.schema.where ?
                        React.createElement(OptionalWrapper, {innerComponent: whereSelectors, remove: {where: {$set: null}}, 
                                         add: {where: {$set: {}}}, name: "Filters", display: this.state.current.where ? true : false, 
                                         updater: this.handleInput}) : null
                ), 
                React.createElement(FetchPlot, {submit: this.state.current.submit, handlePlotClick: this.handlePlotClick})
            )
        );
    }
});

var GeomArray = React.createClass({displayName: "GeomArray",
    render: function() {
        return (
        React.createElement("div", null, 
            React.createElement("div", {className: "row"}, 
                React.createElement("div", {className: "col-md-4"}, 
                    React.createElement(AesPresetRadio, {value: "histogram", thumbnail: "/geom_histogram.png"})
                ), 
                React.createElement("div", {className: "col-md-4"}, 
                    React.createElement(AesPresetRadio, {value: "stacked-bar", thumbnail: "/geom_bar.png"})
                ), 
                React.createElement("div", {className: "col-md-4"}, 
                    React.createElement(AesPresetRadio, {value: "dodged-bar", thumbnail: "/geom_bar.png"})
                )
            ), 
            React.createElement("div", {className: "row"}, 
                React.createElement("div", {className: "col-md-4"}, 
                    React.createElement(AesPresetRadio, {value: "bin2d", thumbnail: "/heatmap.png"})
                ), 
                React.createElement("div", {className: "col-md-4"}, 
                    React.createElement(AesPresetRadio, {value: "point", thumbnail: "/heatmap.png"})
                ), 
                React.createElement("div", {className: "col-md-4"})
            )
        )
        );
    }
});
var root = React.createElement(XvsyRoot);
React.render(root, document.getElementById('content'));
