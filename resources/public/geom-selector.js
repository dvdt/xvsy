/** @jsx React.DOM */
var Geom = React.createClass({
  render: function () {
    return (
      <div className="geom">
        <div className="row"><img src={this.props.thumbnail} width="50px" height="50px" /></div>
        <div className="row">{this.props.name}</div>
      </div>
    )
  }
});

var GeomHist = React.createClass({
  handleClick: function() {console.log("hisssts");},
  render: function () {
    return (
        <button onClick={this.props.handleClick} className="btn btn-large"><div className="row"><img src="/geom_histogram.png" width="50px" height="50px" /></div>
          <div className="row">hist</div></button>
    )
  }
});

var AestheticForm = React.createClass({
    render: function () {
        var column_options = this.props.columns.map(function (c) {
            return (
                <option value={c}>{c}</option>
            )
        });
        return (
                <div>
                <label forName={this.props.aes_name+"-col"}>{this.props.aes_name}</label>
                <select id={this.props.aes_name+"-col"}>
                {column_options}</select>
                <label >Position</label>
                </div>
                
        );
    }
});

var CcplotForm = React.createClass({
  render: function() {
      var data = ['a', 'b', 'c', 'd'];
      var options = data.map(function (x) {
          return (
              <option value={x}>{x}</option>
          );
      });
      var cols = ["c1", "c2", "c3"];
      return (
              <form>
              <AestheticForm columns={cols} aes_name="my-x-aes"/>
              </form>
      )
  }
});

var ClickMe = React.createClass({
});

var GeomSelector = React.createClass({
  render: function() {
    return (
      <div className="geomSelector">
        <div className="row">
          <GeomHist handleClick={function() {console.log("asdf");}}/>
          <Geom name="bar" thumbnail="/geom_bar.png" />
          <Geom name="2d bin" thumbnail="heatmap.png" />

        </div>
          <CcplotForm />
      </div>
    );
  }
});


React.renderComponent(
  GeomSelector(null),
  document.getElementById('content')
)
