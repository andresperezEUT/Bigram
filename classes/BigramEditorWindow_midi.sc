////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) Andrés Pérez López, November 2014 [contact@andresperezlopez.com]
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; withot even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <<http://www.gnu.org/licenses/>
//
////////////////////////////////////////////////////////////////////////////

+ BigramEditorWindow {

	configureMidi {

		midiInstruments = this.loadMidiNames;

		/////////// MIDI OUT //////////

		switch(thisProcess.platform.name)
		{\linux} {
				var fluidIndex;


				// start qsynth
				"qsynth".unixCmd; // if it's open it won't do nothing

				// init midiClient if it was not instanciated
				if (MIDIClient.sources.isNil) {
					MIDIClient.init;
				};
				midiOut = MIDIOut(0); // TODO: check for compatibility!!

				// 5.wait; //give it some time...

				// get midi index from fluidSynth
				MIDIClient.destinations.do{|d,i|
					var name = d.name.postln;
					name = name.split($ );
					if (name[0] == "FLUID") {fluidIndex = i}
				};

				// connect (from insice sc)
				midiOut.connect(fluidIndex);
		}
		{\windows} {
			// init midiClient if it was not instanciated
			if (MIDIClient.sources.isNil) {
				MIDIClient.init;
			};

			// connect to the default midi synthesizer (only proved in windows 7)
			midiOut = MIDIOut.newByName("Microsoft MIDI Mapper","Microsoft MIDI Mapper");
		}
		{\osx} {
			// init midiClient if it was not instanciated
			if (MIDIClient.sources.isNil) {
				MIDIClient.init;
			};

			// connect to the default midi synthesizer (not proved!!)
			midiOut = MIDIOut(0);
		}
		;
	}

}

// MIDIClient.sources
////////// comment below
/*
"qsynth".unixCmd;
MIDIClient.init
m = MIDIOut(0);

MIDIClient.restart
MIDIClient.destinations.do(_.postln)
~dest=MIDIClient.destinations;
~dest.do{|d,i|
	var name = d.name;
	name = name.split($ );
	if (name[0] == "FLUID") {i.postln}
}
*/
